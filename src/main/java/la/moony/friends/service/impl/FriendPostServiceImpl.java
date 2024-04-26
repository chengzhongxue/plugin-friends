package la.moony.friends.service.impl;


import la.moony.friends.extension.CronFriendPost;
import la.moony.friends.extension.Friend;
import la.moony.friends.extension.FriendPost;
import la.moony.friends.service.BlogCrawlerService;
import la.moony.friends.service.FriendPostService;
import la.moony.friends.util.CommonUtils;
import la.moony.friends.vo.MonthPublish;
import la.moony.friends.vo.RSSInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.index.query.QueryFactory;
import run.halo.app.extension.router.selector.FieldSelector;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static run.halo.app.extension.index.query.QueryFactory.all;
import static run.halo.app.extension.index.query.QueryFactory.and;
import static run.halo.app.extension.index.query.QueryFactory.equal;

@Component
public class FriendPostServiceImpl implements FriendPostService {

    private static final Logger log = LoggerFactory.getLogger(FriendPostServiceImpl.class);

    private final ReactiveExtensionClient client;

    private final BlogCrawlerService blogCrawlerService;


    private final int pageSize = 20;

    public FriendPostServiceImpl(ReactiveExtensionClient client, BlogCrawlerService blogCrawlerService) {
        this.client = client;
        this.blogCrawlerService = blogCrawlerService;
    }


    public Mono<Void> synchronizationFriend() {
        var listOptions = new ListOptions();
        var query = and(all(), QueryFactory.or(
            equal("spec.submittedType",Friend.Spec.SubmittedType.APPROVED.name()),
            equal("spec.submittedType",Friend.Spec.SubmittedType.SYSTEM_CHECK_VALID.name())
        ));
        listOptions.setFieldSelector(FieldSelector.of(query));
        return client.listAll(Friend.class, listOptions, defaultSort())
            .flatMap(friend -> {
                String rssUrl = friend.getSpec().getRssUrl();
                String link = friend.getSpec().getLink();
                String url;
                if (StringUtils.isNotEmpty(link)) {
                    url = link;
                } else {
                    url = rssUrl;
                }
                return isStatusOkByName(friend.getMetadata().getName())
                    .flatMap(statusOk->{
                        if (statusOk){
                            Mono<Integer> integerMono = getSuccessfulRetainLimit();
                            String finalUrl = url;
                            return integerMono.flatMap(sum -> {
                                RSSInfo rssInfo = blogCrawlerService.getRSSInfoByRSSAddress(rssUrl, sum);
                                return savePosts(rssInfo,friend,finalUrl);
                            });
                        }else {
                            return Mono.just(false);
                        }
                    });
            })
            .then();
    }

    public Mono<Boolean> savePosts(RSSInfo rssInfo, Friend friend, String finalUrl) {
        if (rssInfo != null) {
            return Flux.fromIterable(rssInfo.getBlogPosts())
                .filter(blogPost -> isValidNewPost(blogPost))
                .flatMap(blogPost -> {
                var listOptions = new ListOptions();
                var query = equal("spec.link",blogPost.getSpec().getLink());
                listOptions.setFieldSelector(FieldSelector.of(query));
                  return client.listAll(FriendPost.class,listOptions, null).count()
                    .map(Long::intValue).flatMap(size->{
                        if (!(size>0)){
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
                            return client.create(friendPost).thenReturn(true);
                        }else {
                            return Mono.just(false);
                        }
                      });
                })
                .collectList()
                .flatMap(savedPosts -> {
                    if (!savedPosts.isEmpty()) {
                        long count = savedPosts.stream().filter(b -> b.equals(true)).count();
                        if (count>0){
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
                            log.info("posts saved success, blogDomainName: {}", finalUrl);
                            return client.update(friend).thenReturn(true);
                        }else {
                            log.info("no new posts saved, blogDomainName: {}", finalUrl);
                            return Mono.just(false);
                        }
                    } else {
                        log.info("no new posts saved, blogDomainName: {}", finalUrl);
                        return Mono.just(false);
                    }
                });
        } else {
            if (friend.getSpec().getStatus() == null) {
                friend.getSpec().setPullTime(new Date());
                friend.getSpec().setStatus(2);
                return client.update(friend).thenReturn(false);
            } else {
                if (friend.getSpec().getStatus() != 2) {
                    friend.getSpec().setPullTime(new Date());
                    friend.getSpec().setStatus(2);
                    return client.update(friend).thenReturn(false);
                }
            }
            log.info("no new posts saved, blogDomainName: {}", finalUrl);
            return Mono.just(false);
        }
    }

    public Mono<Map<String, Object>> yearlyPublishData(String friendName) {
        List<String> recentYearMonths = getRecentYearMonths();
        var listOptions = new ListOptions();
        listOptions.setFieldSelector(FieldSelector.of(equal("spec.friendName", friendName)));
        Flux<FriendPost> friendPostFlux = client.listAll(FriendPost.class, listOptions, null);

        return Flux.fromIterable(recentYearMonths)
            .concatMap(month -> countPostsForMonthAsync(friendPostFlux, month)
                .map(result -> new AbstractMap.SimpleEntry<>(month, result)))
            .collectList()
            .map(results -> {
                results.sort(Comparator.comparing(entry -> recentYearMonths.indexOf(entry.getKey())));
                List<String> months = results.stream().map(Map.Entry::getKey).collect(Collectors.toList());
                List<Long> postCounts = results.stream().map(entry -> entry.getValue().getCount()).collect(Collectors.toList());
                Map<String, Object> map = new HashMap<>();
                map.put("months", months);
                map.put("postCounts", postCounts);
                return map;
            });
    }

    public Mono<MonthPublish> countPostsForMonthAsync(Flux<FriendPost> friendPostFlux, String month) {
        return friendPostFlux
            .filter(post -> {
                Instant postInstant = post.getSpec().getPubDate();
                YearMonth postYearMonth = YearMonth.from(postInstant.atZone(ZoneId.systemDefault()));
                return postYearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")).equals(month);
            })
            .count()
            .map(count -> new MonthPublish(month, count));
    }

    public List<String> getRecentYearMonths() {
        List<String> yearMonths = new ArrayList<>();
        YearMonth currentYearMonth = YearMonth.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (int i = 11; i >= 0; i--) {
            YearMonth targetYearMonth = currentYearMonth.minusMonths(i);
            String formattedMonth = targetYearMonth.format(formatter);
            yearMonths.add(formattedMonth);
        }
        return yearMonths;
    }


    private boolean isValidNewPost(FriendPost blogPost) {
        Instant publishedAt = blogPost.getSpec().getPubDate();
        return publishedAt.isBefore(Instant.now());
    }


    public Mono<Integer> getSuccessfulRetainLimit(){
        return client.fetch(CronFriendPost.class, "cron-default")
            .flatMap(cronFriendPost -> {
                int successfulRetainLimit =
                    cronFriendPost.getSpec().getSuccessfulRetainLimit();
                if (successfulRetainLimit == 0) {
                    successfulRetainLimit = 5;
                }
                return Mono.just(successfulRetainLimit);
            }).defaultIfEmpty(5);
    }


    public Mono<Boolean> isStatusOkByName(String name) {
        return client.fetch(Friend.class, name).flatMap(friend -> {
            boolean b = true;
            if (friend.getStatus() == null){
                b = true;
            }else {
                Friend.Status.StatusType statusType = friend.getStatus().getStatusType();
                if (statusType ==null){
                    b =  true;
                }else {
                    b =   statusType.equals(Friend.Status.StatusType.OK);
                }
            }
            return Mono.just(b);
        }).defaultIfEmpty(true);
    }
    static Sort defaultSort() {
        return Sort.by("metadata.creationTimestamp").descending();
    }
}
