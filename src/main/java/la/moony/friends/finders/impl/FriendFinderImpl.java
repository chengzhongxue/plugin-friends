package la.moony.friends.finders.impl;

import jakarta.annotation.Nonnull;
import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Function;
import la.moony.friends.extension.Friend;
import la.moony.friends.extension.FriendPost;
import la.moony.friends.finders.FriendFinder;
import la.moony.friends.vo.FriendPostVo;
import la.moony.friends.vo.FriendVo;
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
    

    private final ReactiveExtensionClient client;



    Flux<FriendPost> friendPostList(@Nullable Predicate<FriendPost> predicate) {
        return client.list(FriendPost.class, predicate, defaultComparator());
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
        return friendList(null).map(FriendVo::from);
    }


    @Override
    public Mono<ListResult<FriendPostVo>> list(Integer page, Integer size) {
        return pageFriendPost(page, size, defaultComparator());
    }


    @Override
    public Mono<ListResult<FriendVo>> friendList(Integer page, Integer size) {
        return pageFriend(page, size, defaultFriendComparator());
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
    public Mono<FriendPostVo> get(String friendPostName) {
        return client.get(FriendPost.class, friendPostName)
            .filter(null)
            .flatMap(this::getFriendPostVo);
    }

    @Override
    public Mono<FriendVo> friendGet(String friendName) {
        return client.get(Friend.class, friendName)
            .filter(null)
            .flatMap(this::getFriendVo);
    }


    private Mono<ListResult<FriendPostVo>> pageFriendPost(Integer page, Integer size,
        Comparator<FriendPost> comparator) {
        return client.list(FriendPost.class, null, comparator,
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

    private Mono<ListResult<FriendVo>> pageFriend(Integer page, Integer size, Comparator<Friend> comparator) {
        return client.list(Friend.class, null, comparator,
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

    private Comparator<FriendPost> defaultComparator() {
        Function<FriendPost, Instant> pubDate =
            friendPost -> friendPost.getSpec().getPubDate();
        Function<FriendPost, String> name = post -> post.getMetadata().getName();
        return Comparator.comparing(pubDate, Comparators.nullsLow())
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
}
