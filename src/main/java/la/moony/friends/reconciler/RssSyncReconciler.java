package la.moony.friends.reconciler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import la.moony.friends.RssFeedSyncEvent;
import la.moony.friends.extension.CronFriendPost;
import la.moony.friends.extension.FriendPost;
import la.moony.friends.extension.Link;
import la.moony.friends.extension.RssFeedSyncLog;
import la.moony.friends.util.CommonUtils;
import la.moony.friends.util.LinkRequest;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.Unstructured;
import run.halo.app.extension.controller.*;
import run.halo.app.extension.router.selector.FieldSelector;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.data.domain.Sort.Order.asc;
import static run.halo.app.extension.MetadataUtil.nullSafeAnnotations;
import static run.halo.app.extension.index.query.QueryFactory.and;
import static run.halo.app.extension.index.query.QueryFactory.equal;
import static run.halo.app.extension.index.query.QueryFactory.isNull;

@Component
public class RssSyncReconciler implements Reconciler<RssSyncReconciler.Request>,SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(RssSyncReconciler.class);

    private volatile boolean running = false;

    private final ExtensionClient client;
    private final RequestQueue<Request> queue;
    private final Controller controller;
    private final ObjectMapper objectMapper = Unstructured.OBJECT_MAPPER;


    public RssSyncReconciler(ExtensionClient client) {
        this.client = client;
        queue = new DefaultQueue<>(Instant::now);
        controller = this.setupWith(null);
    }

    @Override
    public Result reconcile(Request request) {
        Link link = request.link();
        int sum = request.sum();
        List<String> disableSyncList = request.disableSyncList();
        var linkName = link.getMetadata().getName();
        String rssUrl = getRss(link);
        boolean isContains = false;
        if (ObjectUtils.isNotEmpty(disableSyncList)) {
            isContains = disableSyncList.contains(linkName);
        }
        String syncLogName = "sync-log-" + linkName;
        RssFeedSyncLog newRssFeedSyncLog = new RssFeedSyncLog();
        Metadata metadata = new Metadata();
        metadata.setName(syncLogName);
        newRssFeedSyncLog.setMetadata(metadata);
        newRssFeedSyncLog.setLinkName(linkName);
        if (StringUtils.isNotEmpty(rssUrl) && !isContains) {
            tryToSynchronizeFriendPost(newRssFeedSyncLog, link, sum);
        }else if (StringUtils.isEmpty(rssUrl)) {
            newRssFeedSyncLog.setSyncTime(Instant.now());
            newRssFeedSyncLog.setState(RssFeedSyncLog.RssFeedSyncLogState.nolink);
            newRssFeedSyncLog.setFailureReason("No RSS link");
            newRssFeedSyncLog.setFailureMessage("无 RSS 链接.");
        }else if (isContains) {
            newRssFeedSyncLog.setSyncTime(Instant.now());
            newRssFeedSyncLog.setState(RssFeedSyncLog.RssFeedSyncLogState.failed);
            newRssFeedSyncLog.setFailureReason("admin disables sync");
            newRssFeedSyncLog.setFailureMessage("管理员禁用同步.");
        }
        Optional<RssFeedSyncLog> fetch = client.fetch(RssFeedSyncLog.class, syncLogName);
        if (fetch.isPresent()) {
            RssFeedSyncLog rssFeedSyncLog = fetch.get();
            rssFeedSyncLog.setState(newRssFeedSyncLog.getState());
            rssFeedSyncLog.setSyncTime(newRssFeedSyncLog.getSyncTime());
            rssFeedSyncLog.setFailureReason(newRssFeedSyncLog.getFailureReason());
            rssFeedSyncLog.setFailureMessage(newRssFeedSyncLog.getFailureMessage());
            client.update(rssFeedSyncLog);
        }else {
            client.create(newRssFeedSyncLog);
        }
        return Result.doNotRetry();
    }

    public String getRss(Link link) {
        var annotations = nullSafeAnnotations(link);

        //处理旧数据 rss_uri
        String rssUri = annotations.get("rss_uri");

        String rssUrl = annotations.get("rss_url");
        String isRequest = annotations.get("is_request");
        if ((StringUtils.isEmpty(rssUrl) && StringUtils.isEmpty(isRequest)) || StringUtils.isNotEmpty(rssUri)) {
            String linkRss = LinkRequest.getLinkRss(link.getSpec().getUrl());
            if (StringUtils.isNotEmpty(rssUri)) {
                linkRss = rssUri;
                annotations.remove("rss_uri");
                annotations.put("rss_url", rssUri);
            }else {
                annotations.put("is_request","true");
                if (StringUtils.isNotEmpty(linkRss)) {
                    annotations.put("rss_url",linkRss);
                }
            }
            updateLink(link);
            return linkRss;
        }
        return rssUrl;
    }


    public void updateLink(Link link) {
        Map extensionMap = objectMapper.convertValue(link, Map.class);
        var extension = new Unstructured(extensionMap);
        client.update(extension);
    }

    private void tryToSynchronizeFriendPost(RssFeedSyncLog syncLog, Link link, int sum) {
        var linkName = link.getMetadata().getName();
        var annotations = nullSafeAnnotations(link);
        var rssUrl = annotations.get("rss_url");

        CronFriendPost cron = new CronFriendPost();
        var spec = new CronFriendPost.CronSpec();
        spec.setSuccessfulRetainLimit(5);
        cron.setSpec(spec);
        syncLog.setSyncTime(Instant.now());
        List<FriendPost> friendPostList = fetchFriendPost(rssUrl, sum, syncLog);
        
        // 即使获取失败也继续处理
        if (friendPostList != null && !friendPostList.isEmpty()) {
            for (FriendPost rssPost : friendPostList) {
                var friendPostListOptions = new ListOptions();
                friendPostListOptions.setFieldSelector(FieldSelector.of(
                    and(isNull("metadata.deletionTimestamp"),
                    equal("spec.postLink",rssPost.getSpec().getPostLink())
                    )
                ));
                long total = client.listBy(FriendPost.class, friendPostListOptions,
                    PageRequestImpl.ofSize(1)).getTotal();
                if (total==0) {
                    FriendPost friendPost = new FriendPost();
                    // 设置元数据才能保存
                    friendPost.setMetadata(new Metadata());
                    friendPost.getMetadata().setGenerateName("friend-post-");
                    friendPost.setSpec(rssPost.getSpec());
                    var friendPostSpec = friendPost.getSpec();
                    friendPostSpec.setLogo(link.getSpec().getLogo());
                    friendPostSpec.setAuthor(link.getSpec().getDisplayName());
                    friendPostSpec.setAuthorUrl(link.getSpec().getUrl());
                    friendPostSpec.setLinkName(linkName);
                    String description = friendPost.getSpec().getDescription();
                    //解析html内容转换成文本
                    if (StringUtils.isNotEmpty(description)){
                        description = CommonUtils.parseAndTruncateHtml2Text(description, 200);
                        String regexp = "[　*|\\s*]*";
                        description = description.replaceFirst(regexp, "").trim();
                        friendPostSpec.setDescription(description);
                    }
                    client.create(friendPost);
                }
            }
            delFriendPost(linkName,sum);
        }
    }

    public List<FriendPost> fetchFriendPost(String rssAddress,int postsLimit, RssFeedSyncLog sync) {
        List<FriendPost> friendPostList = new ArrayList<>();
        try {
            SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL(rssAddress)));
            int postCount = 0;
            for (SyndEntry entry : feed.getEntries()) {
                Date date = entry.getPublishedDate();
                if (date == null) {
                    date = entry.getUpdatedDate();
                }
                String description = "";
                SyndContent content = entry.getDescription();
                if (content != null) {
                    description = content.getValue();
                }else {
                    List<SyndContent> contents = entry.getContents();
                    if (!contents.isEmpty()) {
                        description = contents.get(0).getValue();
                    }
                }

                FriendPost friendPost = new FriendPost();
                friendPost.setSpec(new FriendPost.FriendPostSpec());
                friendPost.getSpec().setTitle(entry.getTitle());
                friendPost.getSpec().setPostLink(entry.getLink());
                friendPost.getSpec().setPubDate(date.toInstant());
                friendPost.getSpec().setDescription(description);
                friendPostList.add(friendPost);
                postCount++;
                if (postCount >= postsLimit) {
                    break;
                }
            }
            sync.setState(RssFeedSyncLog.RssFeedSyncLogState.success);
        } catch (Exception e) {
            sync.setState(RssFeedSyncLog.RssFeedSyncLogState.failed);
            sync.setFailureReason("CrawlingRSSError");
            sync.setFailureMessage(e.getMessage());
            log.error("error in crawling rss", e);
            return null;
        }
        return friendPostList;
    }

    public void delFriendPost(String linkName,Integer sum) {
        var friendPostListOptions = new ListOptions();
        friendPostListOptions.setFieldSelector(FieldSelector.of(
            equal("spec.linkName",linkName)
        ));
        long total = client.listBy(FriendPost.class, friendPostListOptions, PageRequestImpl.ofSize(1)).getTotal();
        if (total > sum) {
            int l = (int) (total - sum);
            PageRequestImpl pageRequestImpl = PageRequestImpl.of(pageNullSafe(1), sizeNullSafe(l),
                Sort.by(asc("spec.pubDate")));
            ListResult<FriendPost> friendPosts =
                client.listBy(FriendPost.class, friendPostListOptions, pageRequestImpl);
            for (FriendPost friendPost : friendPosts) {
                client.delete(friendPost);
            }
        }
    }

    static int pageNullSafe(Integer page) {
        return ObjectUtils.defaultIfNull(page, 1);
    }

    static int sizeNullSafe(Integer size) {
        return ObjectUtils.defaultIfNull(size, 10);
    }


    @Override
    public Controller setupWith(ControllerBuilder builder) {
        return new DefaultController<>(
            this.getClass().getName(),
            this,
            queue,
            null,
            Duration.ofMillis(300),
            Duration.ofMinutes(5),
            5);
    }

    @Override
    public void start() {
        this.controller.start();
        this.running = true;
    }

    @Override
    public void stop() {
        this.running = false;
        this.controller.dispose();
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @EventListener(RssFeedSyncEvent.class)
    public void onReplyEvent(RssFeedSyncEvent event) {
        var request = new Request(event.getLink(),event.getSum(),event.getDisableSyncList());
        queue.addImmediately(request);
    }

    public record Request(Link link,int sum,List<String> disableSyncList) {
    }
}
