package la.moony.friends.service.impl;


import la.moony.friends.extension.CronFriendPost;
import la.moony.friends.extension.Friend;
import la.moony.friends.extension.FriendPost;
import la.moony.friends.finders.FriendFinder;
import la.moony.friends.rest.FriendPostController;
import la.moony.friends.service.BlogCrawlerService;
import la.moony.friends.service.BlogStatusService;
import la.moony.friends.service.FriendPostService;
import la.moony.friends.util.CommonUtils;
import la.moony.friends.vo.RSSInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.comparator.Comparators;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.Metadata;
import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

@Component
public class FriendPostServiceImpl implements FriendPostService {

    private static final Logger log = LoggerFactory.getLogger(FriendPostController.class);

    private final ExtensionClient client;

    private final FriendFinder friendFinder;

    private final BlogStatusService blogStatusService;

    private final BlogCrawlerService blogCrawlerService;


    private final int pageSize = 20;

    public FriendPostServiceImpl(FriendFinder friendFinder , ExtensionClient client,
        BlogStatusService blogStatusService, BlogCrawlerService blogCrawlerService) {
        this.client = client;
        this.friendFinder = friendFinder;
        this.blogStatusService = blogStatusService;
        this.blogCrawlerService = blogCrawlerService;
    }


    @Override
    public void synchronizationFriend() {

        Predicate<Friend> paramPredicate = friend ->
        {
            if (friend.getSpec().getSubmittedType()==null) {
                return true;
            }else {
                return  Objects.equals(friend.getSpec().getSubmittedType(),Friend.Spec.SubmittedType.APPROVED) ||
                    Objects.equals(friend.getSpec().getSubmittedType(),Friend.Spec.SubmittedType.SYSTEM_CHECK_VALID);
            }
        };

        ListResult<Friend> listResult = client.list(Friend.class, paramPredicate, null, 1, pageSize);
        //分页数据
        //分页获取并处理
        for (int i = 1; i <= listResult.getTotalPages(); i++) {
            ListResult<Friend> friendsPage = client.list(Friend.class, paramPredicate, defaultComparator(), i, pageSize);
            for (Friend friend : friendsPage) {
                String rssUrl = friend.getSpec().getRssUrl();
                String link = friend.getSpec().getLink();
                String url = rssUrl;
                if (StringUtils.isNotEmpty(link)){
                    url = link;
                }
                boolean statusOk = blogStatusService.isStatusOkByName(friend.getMetadata().getName());
                if (statusOk) {
                    log.info("start crawling posts, blogDomainName: {}", url);
                    Optional<CronFriendPost> cronFriendPost =
                        client.fetch(CronFriendPost.class, "cron-default");
                    int successfulRetainLimit = 0;
                    if (cronFriendPost.isPresent()){
                        successfulRetainLimit = cronFriendPost.get().getSpec().getSuccessfulRetainLimit();
                        if (successfulRetainLimit==0){
                            successfulRetainLimit = 10;
                        }
                    }else {
                        successfulRetainLimit = 10;
                    }
                    RSSInfo rssInfo =
                        blogCrawlerService.getRSSInfoByRSSAddress(rssUrl, successfulRetainLimit);

                    boolean success = savePosts(friend.getSpec().getLink(), rssInfo, friend);
                    if (!success) {
                        log.info("no new posts saved, blogDomainName: {}", url);
                        continue;
                    }
                    log.info("posts saved success, blogDomainName: {}", url);
                }
            }
        }
    }

    public boolean savePosts(String blogDomainName, RSSInfo rssInfo,Friend friend) {
        if (null != rssInfo) {
            int saveCount = 0;
            for (FriendPost blogPost : rssInfo.getBlogPosts()) {
                String link = blogPost.getSpec().getLink();
                boolean existsByLink = existsByLink(link);

                Instant publishedAt = blogPost.getSpec().getPubDate();
                boolean isValidNewPost = publishedAt.isBefore(Instant.now());

                if (!existsByLink && isValidNewPost) {
                    FriendPost friendPost = new FriendPost();
                    // 设置元数据才能保存
                    friendPost.setMetadata(new Metadata());
                    friendPost.getMetadata().setGenerateName("friend-post-");
                    friendPost.setSpec(blogPost.getSpec());
                    friendPost.getSpec().setLogo(friend.getSpec().getLogo());
                    friendPost.setStatus(new FriendPost.Status());
                    friendPost.getSpec().setFriendName(friend.getMetadata().getName());
                    Friend.Spec spec = friend.getSpec();
                    if (StringUtils.isNotEmpty(spec.getDisplayName())){
                        friendPost.getSpec().setAuthor(spec.getDisplayName());
                    }
                    String description = friendPost.getSpec().getDescription();
                    //解析html内容转换成文本
                    if (StringUtils.isNotEmpty(description)){
                        description = CommonUtils.parseAndTruncateHtml2Text(description, 800);
                        String regexp = "[　*|\\s*]*";
                        description = description.replaceFirst(regexp, "").trim();
                        friendPost.getSpec().setDescription(description);
                    }
                    saveCount = saveCount+1;
                    client.create(friendPost);
                }
            }
            boolean b = saveCount > 0;
            if (b){
                Friend.Spec spec = friend.getSpec();
                if (!StringUtils.isNotEmpty(spec.getLink())){
                    friend.getSpec().setLink(rssInfo.getBlogAddress());
                }
                if (!StringUtils.isNotEmpty(spec.getDescription())){
                    friend.getSpec().setDescription(rssInfo.getBlogDescription());
                }
                if (!StringUtils.isNotEmpty(spec.getDisplayName())){
                    friend.getSpec().setDisplayName(rssInfo.getBlogTitle());
                }
                friend.getSpec().setStatus(1);
                if (friend.getStatus()==null){
                    friend.setStatus(new Friend.Status());
                }
                friend.getStatus().setStatusType(Friend.Status.StatusType.OK);
                friend.getSpec().setPullTime(new Date());
                friend.getSpec().setUpdateTime(rssInfo.getBlogPosts().get(0).getSpec().getPubDate());
                if (friend.getSpec().getSubmittedType()==null){
                    friend.getSpec().setSubmittedType(Friend.Spec.SubmittedType.APPROVED);
                    friend.getSpec().setReason("审核通过");
                }
                client.update(friend);
            }
            return b;
        }else {
            if (friend.getSpec().getStatus()==null){
                friend.getSpec().setPullTime(new Date());
                friend.getSpec().setStatus(2);
                client.update(friend);
            }else {
                if (friend.getSpec().getStatus()!=2){
                    friend.getSpec().setPullTime(new Date());
                    friend.getSpec().setStatus(2);
                    client.update(friend);
                }
            }
            return false;
        }

    }

    public boolean existsByLink(String link) {
        int size = client.list(FriendPost.class,
            post -> StringUtils.equals(post.getSpec().getLink(), link), null).size();
        return size>0;
    }


    private Comparator<Friend> defaultComparator() {
        Function<Friend, Instant> pubDate =
            friendPost -> friendPost.getMetadata().getCreationTimestamp();
        Function<Friend, String> name = post -> post.getMetadata().getName();
        return Comparator.comparing(pubDate, Comparators.nullsLow())
            .thenComparing(name);
    }
}
