package la.moony.friends.finders.impl;

import jakarta.annotation.Nonnull;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Function;
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
import org.springframework.lang.Nullable;
import org.springframework.util.comparator.Comparators;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.theme.finders.Finder;


/**
 * A default implementation for {@link FriendFinder}.
 *
 * @author LIlGG
 * @since 1.0.0
 */
@Finder("friendFinder")
@RequiredArgsConstructor
public class FriendFinderImpl implements FriendFinder {

    public static final Predicate<Friend> FRIEND_PREDICATE = friend ->
    {
        if (friend.getSpec().getSubmittedType()==null) {
            return true;
        }else {
            return  Objects.equals(friend.getSpec().getSubmittedType(),Friend.Spec.SubmittedType.APPROVED) ||
                Objects.equals(friend.getSpec().getSubmittedType(),Friend.Spec.SubmittedType.SYSTEM_CHECK_VALID);
        }
    };

    private final ReactiveExtensionClient client;



    Flux<FriendPost> friendPostList(@Nullable Predicate<FriendPost> predicate) {
        return client.list(FriendPost.class, predicate, defaultFriendPostComparator());
    }


    Flux<Friend> friendList(@Nullable Predicate<Friend> predicate) {
        return client.list(Friend.class, predicate, defaultFriendComparator());
    }

    @Override
    public Flux<FriendPostVo> listAll() {
        return friendPostList(null).map(FriendPostVo::from);
    }

    @Override
    public Flux<FriendVo> friendListAll() {
        return friendList(FRIEND_PREDICATE).map(FriendVo::from);
    }


    @Override
    public Mono<ListResult<FriendPostVo>> list(Integer page, Integer size) {
        return pageFriendPost(page, size,null, defaultFriendPostComparator());
    }

    @Override
    public Mono<ListResult<FriendPostVo>> list(Integer page, Integer size,String keyword) {
        return pageFriendPost(page, size,
            postToPredicate(keyword),
            defaultFriendPostComparator()
        );
    }


    @Override
    public Mono<ListResult<FriendVo>> friendList(Integer page, Integer size) {
        return pageFriend(page, size,null, defaultFriendComparator());
    }

    @Override
    public Mono<ListResult<FriendPostVo>> listByUrl(Integer page, Integer size,String url) {
        return pageFriendPost(page, size,post -> StringUtils.equals(post.getSpec().getUrl(), url), defaultFriendPostComparator());
    }

    @Override
    public Flux<FriendPostVo> listByUrl(String url) {
        return friendPostList(post -> StringUtils.equals(post.getSpec().getUrl(), url))
            .map(FriendPostVo::from);
    }

    @Override
    public Flux<FriendPostVo> listByAuthor(String author) {
        return friendPostList(post -> StringUtils.equals(post.getSpec().getAuthor(), author))
            .map(FriendPostVo::from);
    }

    @Override
    public Mono<ListResult<FriendPostVo>> listByAuthor(Integer page, Integer size,String author) {
        return pageFriendPost(page, size,post -> StringUtils.equals(post.getSpec().getAuthor(), author), defaultFriendPostComparator());
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
        return pageFriend(pageNum, pageSize,
            blogToPredicate(keyword),
            defaultBlogComparator(sort)
        );
    }

    @Override
    public Mono<ListResult<BlogVo>> blogList(int pageNum, Integer pageSize, String sort,
        String keyword) {
        return pageBlog(pageNum, pageSize,
            blogToPredicate(keyword),
            defaultBlogComparator(sort)
        );
    }

    @Override
    public Mono<ListResult<FriendVo>> blogRequestList(Integer page, Integer size) {
        return pageFriend(page, size,friend -> friend.getSpec().getSelfSubmitted(),
            defaultFriendComparator());
    }

    public Predicate<Friend> blogToPredicate(String keyword) {
        Predicate<Friend> keywordPredicate = friend -> {
            if (StringUtils.isBlank(keyword)) {
                return true;
            }
            String keywordToSearch = keyword.trim().toLowerCase();
            return StringUtils.containsAnyIgnoreCase(friend.getSpec().getDisplayName(),
                keywordToSearch)
                || StringUtils.containsAnyIgnoreCase(friend.getSpec().getRssUrl(), keywordToSearch)
                || StringUtils.containsAnyIgnoreCase(friend.getSpec().getLink(), keywordToSearch);
        };
        Predicate<Friend> groupPredicate = friend -> {
            if (friend.getSpec().getStatus()!=null){
                return friend.getSpec().getStatus().equals(1);
            }
            return false;
        };
        return groupPredicate.and(keywordPredicate).and(FRIEND_PREDICATE);
    }

    public Predicate<FriendPost> postToPredicate(String keyword) {
        Predicate<FriendPost> keywordPredicate = post -> {
            if (StringUtils.isBlank(keyword)) {
                return true;
            }
            String keywordToSearch = keyword.trim().toLowerCase();
            return StringUtils.containsAnyIgnoreCase(post.getSpec().getTitle(),
                keywordToSearch)
                || StringUtils.containsAnyIgnoreCase(post.getSpec().getDescription(), keywordToSearch)
                || StringUtils.containsAnyIgnoreCase(post.getSpec().getAuthor(), keywordToSearch);
        };
        Predicate<FriendPost> groupPredicate = friend -> {return true;};
        return groupPredicate.and(keywordPredicate);
    }

    @Override
    public Mono<FriendPostVo> get(String friendPostName) {
        return client.get(FriendPost.class, friendPostName)
            .filter(null)
            .flatMap(this::getFriendPostVo);
    }

    @Override
    public Mono<FriendVo> friendGet(String friendName) {
        Predicate<Friend> predicate = friend -> true;
        return client.get(Friend.class, friendName)
            .filter(predicate)
            .flatMap(this::getFriendVo);
    }

    private Mono<ListResult<FriendPostVo>> pageFriendPost(Integer page, Integer size,
        @Nullable Predicate<FriendPost> predicate,
        Comparator<FriendPost> comparator) {
        return client.list(FriendPost.class, predicate, comparator,
                pageNullSafe(page), sizeNullSafe(size))
            .flatMap(list -> Flux.fromStream(list.get())
                .concatMap(this::getFriendPostVo)
                .collectList()
                .map(postVos -> new ListResult<>(list.getPage(), list.getSize(),
                    list.getTotal(), postVos)
                )
            )
            .defaultIfEmpty(new ListResult<>(page, size, 0L, List.of()));
    }

    private Mono<ListResult<FriendVo>> pageFriend(Integer page, Integer size,
        @Nullable Predicate<Friend> predicate,
        Comparator<Friend> comparator) {
        return client.list(Friend.class, predicate, comparator,
                pageNullSafe(page), sizeNullSafe(size))
            .flatMap(list -> Flux.fromStream(list.get())
                .concatMap(this::getFriendVo)
                .collectList()
                .map(friendVos -> new ListResult<>(list.getPage(), list.getSize(),
                    list.getTotal(), friendVos)
                )
            )
            .defaultIfEmpty(new ListResult<>(page, size, 0L, List.of()));
    }

    private Mono<ListResult<BlogVo>> pageBlog(Integer page, Integer size,
        @Nullable Predicate<Friend> predicate,
        Comparator<Friend> comparator) {
        Flux<FriendPost> friendPostFlux = friendPostList(null);
        return client.list(Friend.class, predicate, comparator,
                pageNullSafe(page), sizeNullSafe(size))
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
            .defaultIfEmpty(new ListResult<>(page, size, 0L, List.of()));
    }

    static Comparator<Friend> defaultBlogComparator(String sort) {
        Function<Friend, Instant> function = friend -> friend.getSpec().getUpdateTime();
        if (StringUtils.isNotEmpty(sort)){
            if (sort.equals("collect_time")){
                function = friend -> friend.getMetadata().getCreationTimestamp();
            }
        }
        Function<Friend, String> name = friend -> friend.getMetadata()
            .getName();
        return Comparator.comparing(function, Comparators.nullsLow())
            .thenComparing(name)
            .reversed();
    }

    static Comparator<FriendPost> defaultFriendPostComparator() {
        Function<FriendPost, Instant> function = friend -> friend.getSpec().getPubDate();
        Function<FriendPost, String> name = friendPost -> friendPost.getMetadata()
            .getName();
        return Comparator.comparing(function, Comparators.nullsLow())
            .thenComparing(name)
            .reversed();
    }

    static Comparator<Friend> defaultFriendComparator() {
        Function<Friend, Instant> createTime = friend -> friend.getMetadata()
            .getCreationTimestamp();
        Function<Friend, String> name = friend -> friend.getMetadata()
            .getName();
        return Comparator.comparing(createTime, Comparators.nullsLow())
            .thenComparing(name)
            .reversed();
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
        return client.list(Friend.class, FRIEND_PREDICATE, null)
            .count()
            .map(Long::intValue);
    }

    public Mono<Integer> isFriend(String rssUrl) {
        return client.list(Friend.class,friend -> StringUtils.equals(friend.getSpec().getRssUrl(),rssUrl), null)
            .count()
            .map(Long::intValue);
    }

    public Mono<Integer> friendSucceedCount() {
        return client.list(Friend.class, FRIEND_PREDICATE.and(friend -> {
                    if (friend.getSpec().getStatus()!=null){
                        if (friend.getSpec().getStatus() == 1){
                            return true;
                        }
                    }
                    return false;
                }), null)
            .count()
            .map(Long::intValue);
    }

    public Mono<Integer> friendPostCount() {
        return client.list(FriendPost.class, null, null)
            .count()
            .map(Long::intValue);
    }
}
