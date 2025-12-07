package la.moony.friends.finders.impl;

import jakarta.annotation.Nonnull;
import la.moony.friends.extension.FriendPost;
import la.moony.friends.extension.Link;
import la.moony.friends.extension.LinkGroup;
import la.moony.friends.extension.RssFeedSyncLog;
import la.moony.friends.finders.FriendFinder;
import la.moony.friends.util.SortUtils;
import la.moony.friends.vo.FriendPostVo;
import la.moony.friends.vo.LinkGroupVo;
import la.moony.friends.vo.LinkVo;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.*;
import run.halo.app.extension.router.selector.FieldSelector;
import run.halo.app.infra.utils.JsonUtils;
import run.halo.app.theme.finders.Finder;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.data.domain.Sort.Order.asc;
import static run.halo.app.extension.index.query.QueryFactory.*;


@Finder("friendFinder")
@RequiredArgsConstructor
public class FriendFinderImpl implements FriendFinder {

    static final String UNGROUPED_NAME = "ungrouped";

    private final ReactiveExtensionClient client;



    @Override
    public Flux<FriendPostVo> listAll() {
        var listOptions = new ListOptions();
        var query = all();
        listOptions.setFieldSelector(FieldSelector.of(query));
        return client.listAll(FriendPost.class, listOptions, defaultSort())
            .flatMap(this::getFriendPostVo);
    }

    @Override
    public Mono<ListResult<FriendPostVo>> list(Integer page, Integer size) {
        return pageFriendPost(new ListOptions(), getPageRequest(page, size));
    }

    private PageRequestImpl getPageRequest(Integer page, Integer size) {
        return PageRequestImpl.of(pageNullSafe(page), sizeNullSafe(size), defaultSort());
    }

    @Override
    public Mono<ListResult<FriendPostVo>> list(Map<String, Object> params) {
        var query = Optional.ofNullable(params)
            .map(map -> JsonUtils.mapToObject(map, FriendPostQuery.class))
            .orElseGet(FriendPostQuery::new);
        return pageFriendPost(query.toListOptions(), query.toPageRequest());
    }


    @Override
    public Mono<FriendPostVo> getByName(String friendPostName) {
        return client.fetch(FriendPost.class, friendPostName)
            .map(FriendPostVo::from);
    }

    @Override
    public Flux<LinkGroupVo> linkGroupBy() {
        return client.listAll(LinkGroup.class, new ListOptions(), defaultGroupSort())
            .map(LinkGroupVo::from)
            .concatMap(group -> linkListBy(group.getMetadata().getName())
                .collectList()
                .map(group::withLinks)
                .defaultIfEmpty(group)
            )
            .mergeWith(Mono.defer(() -> linkListBy(UNGROUPED_NAME)
                .collectList()
                // do not return ungrouped group if no links
                .filter(links -> !links.isEmpty())
                .flatMap(links -> ungrouped()
                    .map(LinkGroupVo::from)
                    .map(group -> group.withLinks(links))
                )
            ));
    }

    private Mono<ListResult<FriendPostVo>> pageFriendPost(ListOptions queryOptions, PageRequest page){
        var listOptions = new ListOptions();
        var query = all();
        listOptions.setFieldSelector(FieldSelector.of(query));
        var fieldSelector = queryOptions.getFieldSelector();
        if (fieldSelector != null) {
            listOptions.setFieldSelector(listOptions.getFieldSelector()
                .andQuery(fieldSelector.query()));
        }

        return client.listBy(FriendPost.class, listOptions, page)
            .flatMap(list -> Flux.fromStream(list.get())
                .concatMap(this::getFriendPostVo)
                .collectList()
                .map(friendPostVos -> new ListResult<>(list.getPage(), list.getSize(),
                    list.getTotal(), friendPostVos)
                )
            )
            .defaultIfEmpty(ListResult.emptyResult());

    }

    public Flux<LinkVo> linkListBy(String groupName) {
        var listOptions = new ListOptions();
        var query = isNull("metadata.deletionTimestamp");
        if (UNGROUPED_NAME.equals(groupName)) {
            query = and(query, isNull("spec.groupName"));
        } else {
            query = and(query, equal("spec.groupName", groupName));
        }
        listOptions.setFieldSelector(FieldSelector.of(query));
        return client.listAll(Link.class, listOptions, defaultLinkSort())
            .collectList()
            .flatMap(links -> {
                var linkNames = links.stream()
                    .map(link -> link.getMetadata().getName())
                    .collect(Collectors.toSet());

                var getPosts = getAllFriendPostsByLinkNames(linkNames)
                    .collectMultimap(post -> post.getSpec().getLinkName());

                var getLogs = getByLinkNames(linkNames)
                    .collectMap(syncLog -> syncLog.getLinkName());

                return Mono.zip(getPosts, getLogs)
                    .map(tuple -> {
                        var posts = tuple.getT1();
                        var logs = tuple.getT2();

                        return links.stream()
                            .map(link -> {
                                LinkVo vo = LinkVo.from(link);
                                String name = link.getMetadata().getName();

                                var friendPosts = posts.get(name);
                                if (friendPosts != null && !friendPosts.isEmpty()) {
                                    // 按创建时间排序，最新的在最前面，仅保留前两条
                                    var sortedRecords = friendPosts.stream()
                                        .sorted(defaultFriendPostVoComparator())
                                        .limit(2)
                                        .toList();
                                    vo.setFriendPosts(sortedRecords);
                                } else {
                                    vo.setFriendPosts(List.of());
                                }
                                vo.setRssFeedSyncLog(logs.get(name));
                                return vo;
                            })
                            .sorted(Comparator.comparing((LinkVo link) -> {
                                    List<FriendPostVo> friendPosts = link.getFriendPosts();
                                    if (friendPosts != null && !friendPosts.isEmpty()) {
                                        return friendPosts.get(0).getSpec().getPubDate();
                                    }
                                    return null;
                                }, Comparator.nullsLast(Comparator.reverseOrder())
                            ))
                            .toList();
                    });
            })
            .flatMapMany(Flux::fromIterable);
    }

    static Comparator<FriendPostVo> defaultFriendPostVoComparator() {
        Function<FriendPostVo, Instant>
            pubDate = friendPostVo -> friendPostVo.getSpec().getPubDate();
        return Comparator.comparing(pubDate).reversed();
    }

    public Flux<RssFeedSyncLog> getByLinkNames(Collection<String> linkNames) {
        if (CollectionUtils.isEmpty(linkNames)) {
            return Flux.empty();
        }
        var listOptions = new ListOptions();
        listOptions.setFieldSelector(FieldSelector.of(
            and(
                in("linkName", linkNames),
                isNull("metadata.deletionTimestamp")
            )
        ));
        return client.listAll(RssFeedSyncLog.class, listOptions, ExtensionUtil.defaultSort());
    }


    private Flux<FriendPostVo> getAllFriendPostsByLinkNames(Collection<String> linkNames) {
        if (CollectionUtils.isEmpty(linkNames)) {
            return Flux.empty();
        }
        var listOptions = new ListOptions();
        var query = isNull("metadata.deletionTimestamp");
        query = and(query, in("spec.linkName", linkNames));
        listOptions.setFieldSelector(FieldSelector.of(query));
        return client.listAll(FriendPost.class, listOptions, defaultSort())
            .map(FriendPostVo::from);
    }

    Mono<LinkGroup> ungrouped() {
        LinkGroup linkGroup = new LinkGroup();
        linkGroup.setMetadata(new Metadata());
        linkGroup.getMetadata().setName(UNGROUPED_NAME);
        linkGroup.setSpec(new LinkGroup.LinkGroupSpec());
        linkGroup.getSpec().setDisplayName("");
        linkGroup.getSpec().setPriority(0);
        return Mono.just(linkGroup);
    }

    static Sort defaultGroupSort() {
        return Sort.by(asc("spec.priority"),
            asc("metadata.creationTimestamp"),
            asc("metadata.name")
        );
    }

    static Sort defaultLinkSort() {
        return Sort.by(asc("spec.priority"),
            asc("metadata.creationTimestamp"),
            asc("metadata.name")
        );
    }

    static Sort defaultSort() {
        return Sort.by("spec.pubDate").descending();
    }

    private Mono<FriendPostVo> getFriendPostVo(@Nonnull FriendPost friendPost) {
        FriendPostVo friendPostVo = FriendPostVo.from(friendPost);
        return Mono.just(friendPostVo);
    }

    static int pageNullSafe(Integer page) {
        return ObjectUtils.defaultIfNull(page, 1);
    }

    static int sizeNullSafe(Integer size) {
        return ObjectUtils.defaultIfNull(size, 10);
    }

    @Data
    public static class FriendPostQuery {
        private Integer page;
        private Integer size;
        private String linkName;
        private List<String> sort;

        public ListOptions toListOptions() {
            var builder = ListOptions.builder();
            if (StringUtils.isNotBlank(linkName)) {
                builder.andQuery(equal("spec.linkName", linkName));
            }
            return builder.build();
        }

        public PageRequest toPageRequest() {
            return PageRequestImpl.of(pageNullSafe(getPage()),
                sizeNullSafe(getSize()), SortUtils.resolve(sort).and(defaultSort()));
        }
    }


}
