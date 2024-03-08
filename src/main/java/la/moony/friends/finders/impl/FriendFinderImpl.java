package la.moony.friends.finders.impl;

import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.function.Predicate;
import la.moony.friends.extension.Friend;
import la.moony.friends.extension.FriendPost;
import la.moony.friends.finders.FriendFinder;
import la.moony.friends.vo.BlogVo;
import la.moony.friends.vo.FriendPostVo;
import la.moony.friends.vo.FriendVo;
import la.moony.friends.vo.StatisticalVo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequest;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.index.query.QueryFactory;
import run.halo.app.extension.router.selector.FieldSelector;
import run.halo.app.theme.finders.Finder;

import static run.halo.app.extension.index.query.QueryFactory.all;
import static run.halo.app.extension.index.query.QueryFactory.and;
import static run.halo.app.extension.index.query.QueryFactory.equal;


@Finder("friendFinder")
@RequiredArgsConstructor
public class FriendFinderImpl implements FriendFinder {

    private final ReactiveExtensionClient client;



    @Override
    public Flux<FriendPostVo> listAll() {
        var listOptions = new ListOptions();
        var query = all();
        listOptions.setFieldSelector(FieldSelector.of(query));
        return client.listAll(FriendPost.class, listOptions, defaultFriendPostSort())
            .flatMap(this::getFriendPostVo);
    }

    @Override
    public Flux<FriendVo> friendListAll() {
        var listOptions = new ListOptions();
        var query = and(all(), QueryFactory.or(
            QueryFactory.equal("spec.submittedType",Friend.Spec.SubmittedType.APPROVED.name()),
            QueryFactory.equal("spec.submittedType",Friend.Spec.SubmittedType.SYSTEM_CHECK_VALID.name())
        ));
        listOptions.setFieldSelector(FieldSelector.of(query));
        return client.listAll(Friend.class, listOptions, defaultFriendSort())
            .flatMap(this::getFriendVo);
    }


    @Override
    public Mono<ListResult<FriendPostVo>> list(Integer page, Integer size) {
        var pageRequest = PageRequestImpl.of(pageNullSafe(page), sizeNullSafe(size), defaultFriendPostSort());
        return pageFriendPost(null, pageRequest);
    }

    @Override
    public Mono<ListResult<FriendPostVo>> list(Integer page, Integer size,String keyword) {
        var query = all();
        if (StringUtils.isNotEmpty(keyword)){
            query = and(query, QueryFactory.or(
                QueryFactory.contains("spec.author", keyword),
                QueryFactory.contains("spec.title", keyword),
                QueryFactory.contains("spec.description", keyword)
            ));
        }

        var pageRequest = PageRequestImpl.of(pageNullSafe(page), sizeNullSafe(size), defaultFriendPostSort());
        return pageFriendPost(FieldSelector.of(query), pageRequest);
    }


    @Override
    public Mono<ListResult<FriendVo>> friendList(Integer page, Integer size) {
        var pageRequest = PageRequestImpl.of(pageNullSafe(page), sizeNullSafe(size), defaultFriendSort());
        return pageFriend(null,pageRequest);
    }

    @Override
    public Mono<ListResult<FriendPostVo>> listByUrl(Integer page, Integer size,String url) {
        var query = equal("spec.url", url);
        var pageRequest = PageRequestImpl.of(pageNullSafe(page), sizeNullSafe(size), defaultFriendPostSort());
        return pageFriendPost(FieldSelector.of(query), pageRequest);
    }

    @Override
    public Flux<FriendPostVo> listByUrl(String url) {
        var listOptions = new ListOptions();
        var query = equal("spec.url", url);
        listOptions.setFieldSelector(FieldSelector.of(query));
        return client.listAll(FriendPost.class, listOptions, defaultFriendPostSort())
            .flatMap(this::getFriendPostVo);
    }

    @Override
    public Flux<FriendPostVo> listByAuthor(String author) {
        var listOptions = new ListOptions();
        var query = equal("spec.author", author);
        listOptions.setFieldSelector(FieldSelector.of(query));
        return client.listAll(FriendPost.class, listOptions, defaultFriendPostSort())
            .flatMap(this::getFriendPostVo);
    }

    @Override
    public Mono<ListResult<FriendPostVo>> listByAuthor(Integer page, Integer size,String author) {
        var query = equal("spec.author", author);
        var pageRequest = PageRequestImpl.of(pageNullSafe(page), sizeNullSafe(size), defaultFriendPostSort());
        return pageFriendPost(FieldSelector.of(query), pageRequest);
    }

    @Override
    public Mono<StatisticalVo> statistical() {
        return Mono.just(StatisticalVo.empty()).flatMap(statisticalVo -> friendCount()
            .doOnNext(statisticalVo::setFriendsNum)
            .thenReturn(statisticalVo)
        ).flatMap(statisticalVo -> friendPostCount()
            .doOnNext(statisticalVo::setArticleNum)
            .thenReturn(statisticalVo)
        )
        .flatMap(statisticalVo -> friendSucceedCount()
            .doOnNext(statisticalVo::setActiveNum)
            .thenReturn(statisticalVo));
    }

    @Override
    public Mono<ListResult<FriendVo>> friendList(int pageNum, Integer pageSize, String sort,
        String keyword) {
        var pageRequest = PageRequestImpl.of(pageNullSafe(pageNum), sizeNullSafe(pageSize), defaultBlogSort(sort));
        return pageFriend(blogToPredicate(keyword), pageRequest);
    }

    @Override
    public Mono<ListResult<BlogVo>> blogList(int pageNum, Integer pageSize, String sort,
        String keyword) {
        var pageRequest = PageRequestImpl.of(pageNullSafe(pageNum), sizeNullSafe(pageSize), defaultBlogSort(sort));
        return pageBlog(blogToPredicate(keyword), pageRequest);
    }

    @Override
    public Mono<ListResult<FriendVo>> blogRequestList(Integer page, Integer size) {
        var query = equal("spec.selfSubmitted", "true");
        var pageRequest = PageRequestImpl.of(pageNullSafe(page), sizeNullSafe(size), defaultFriendSort());
        return pageFriend(FieldSelector.of(query), pageRequest);
    }

    public FieldSelector blogToPredicate(String keyword) {
        var query = and(equal("spec.status","1"), QueryFactory.or(
            QueryFactory.equal("spec.submittedType",Friend.Spec.SubmittedType.APPROVED.name()),
            QueryFactory.equal("spec.submittedType",Friend.Spec.SubmittedType.SYSTEM_CHECK_VALID.name())
        ));
        if (StringUtils.isNotBlank(keyword)) {
            query = and(query, QueryFactory.or(
                QueryFactory.contains("spec.displayName", keyword),
                QueryFactory.contains("spec.description", keyword),
                QueryFactory.contains("spec.rssUrl", keyword),
                QueryFactory.contains("spec.link", keyword)
            ));
        }

        return FieldSelector.of(query);
    }

    @Override
    public Mono<FriendPostVo> get(String friendPostName) {
        Predicate<FriendPost> predicate = friend -> true;
        return client.get(FriendPost.class, friendPostName)
            .filter(predicate)
            .flatMap(this::getFriendPostVo);
    }

    @Override
    public Mono<FriendVo> friendGet(String friendName) {
        Predicate<Friend> predicate = friend -> true;
        return client.get(Friend.class, friendName)
            .filter(predicate)
            .flatMap(this::getFriendVo);
    }

    private Mono<ListResult<FriendPostVo>> pageFriendPost(FieldSelector fieldSelector, PageRequest page){
        var listOptions = new ListOptions();
        var query = all();
        if (fieldSelector != null && fieldSelector.query() != null) {
            query = and(query, fieldSelector.query());
        }
        listOptions.setFieldSelector(FieldSelector.of(query));
        return client.listBy(FriendPost.class, listOptions, page)
            .flatMap(list -> Flux.fromStream(list.get())
                .concatMap(this::getFriendPostVo)
                .collectList()
                .map(friendPostVos -> new ListResult<>(list.getPage(), list.getSize(),
                    list.getTotal(), friendPostVos)
                )
            )
            .defaultIfEmpty(
                new ListResult<>(page.getPageNumber(), page.getPageSize(), 0L, List.of()));

    }


    private Mono<ListResult<FriendVo>> pageFriend(FieldSelector fieldSelector, PageRequest page){
        var listOptions = new ListOptions();
        var query = all();
        if (fieldSelector != null && fieldSelector.query() != null) {
            query = and(query, fieldSelector.query());
        }
        listOptions.setFieldSelector(FieldSelector.of(query));
        return client.listBy(Friend.class, listOptions, page)
            .flatMap(list -> Flux.fromStream(list.get())
                .concatMap(this::getFriendVo)
                .collectList()
                .map(friendVos -> new ListResult<>(list.getPage(), list.getSize(),
                    list.getTotal(), friendVos)
                )
            )
            .defaultIfEmpty(
                new ListResult<>(page.getPageNumber(), page.getPageSize(), 0L, List.of()));

    }

    private Mono<ListResult<BlogVo>> pageBlog(FieldSelector fieldSelector, PageRequest page) {
        var listOptionsFriendPost = new ListOptions();
        listOptionsFriendPost.setFieldSelector(FieldSelector.of(all()));
        Flux<FriendPost> friendPostFlux = client.listAll(FriendPost.class, listOptionsFriendPost, defaultFriendPostSort());
        var listOptions = new ListOptions();
        var query = all();
        if (fieldSelector != null && fieldSelector.query() != null) {
            query = and(query, fieldSelector.query());
        }
        listOptions.setFieldSelector(FieldSelector.of(query));
        return client.listBy(Friend.class, listOptions, page)
            .flatMap(list -> Flux.fromStream(list.get()).map(BlogVo::from)
                .concatMap(friend -> friendPostFlux
                    .filter(friendPost -> StringUtils.equals(friendPost.getSpec().getFriendName(),
                        friend.getMetadata().getName())
                    ).map(FriendPostVo::from)
                    .collectList().map(friend::withPosts)
                    .defaultIfEmpty(friend)
                )
                .collectList()
                .map(friendVos -> new ListResult<>(list.getPage(), list.getSize(),
                    list.getTotal(), friendVos)
                )
            )
            .defaultIfEmpty(new ListResult<>(page.getPageNumber(), page.getPageSize(), 0L, List.of()));
    }
    
    static Sort defaultBlogSort(String sort) {
        var sorts = Sort.by("spec.updateTime");
        if (StringUtils.isNotEmpty(sort)){
            if (sort.equals("collect_time")){
                sorts = Sort.by("metadata.creationTimestamp");
            }
        }
        return sorts.descending();
    }

    static Sort defaultFriendPostSort() {
        return Sort.by("spec.pubDate").descending();
    }

    static Sort defaultFriendSort() {
        return Sort.by("metadata.creationTimestamp").descending();
    }

    private Mono<FriendPostVo> getFriendPostVo(@Nonnull FriendPost friendPost) {
        FriendPostVo friendPostVo = FriendPostVo.from(friendPost);
        return Mono.just(friendPostVo);
    }

    private Mono<FriendVo> getFriendVo(@Nonnull Friend friend) {
        FriendVo friendVo = FriendVo.from(friend);
        return Mono.just(friendVo);
    }

    int pageNullSafe(Integer page) {
        return ObjectUtils.defaultIfNull(page, 1);
    }

    int sizeNullSafe(Integer size) {
        return ObjectUtils.defaultIfNull(size, 10);
    }


    public Mono<Integer> friendCount() {
        var listOptions = new ListOptions();
        var query = and(all(), QueryFactory.or(
            QueryFactory.equal("spec.submittedType",Friend.Spec.SubmittedType.APPROVED.name()),
            QueryFactory.equal("spec.submittedType",Friend.Spec.SubmittedType.SYSTEM_CHECK_VALID.name())
        ));
        listOptions.setFieldSelector(FieldSelector.of(query));
        return client.listAll(Friend.class, listOptions, defaultFriendSort())
            .count()
            .map(Long::intValue);
    }

    public Mono<Integer> isFriend(String rssUrl) {
        var listOptions = new ListOptions();
        var query = equal("spec.rssUrl", rssUrl);
        listOptions.setFieldSelector(FieldSelector.of(query));
        return client.listAll(Friend.class, listOptions, defaultFriendSort())
            .count()
            .map(Long::intValue);
    }

    public Mono<Integer> friendSucceedCount() {
        var listOptions = new ListOptions();
        var query = equal("spec.status", "1");
        listOptions.setFieldSelector(FieldSelector.of(query));
        return client.listAll(Friend.class, listOptions, defaultFriendSort())
            .count()
            .map(Long::intValue);
    }

    public Mono<Integer> friendPostCount() {
        var listOptions = new ListOptions();
        listOptions.setFieldSelector(FieldSelector.of(all()));
        return client.listAll(FriendPost.class, listOptions, defaultFriendSort())
            .count()
            .map(Long::intValue);
    }
}
