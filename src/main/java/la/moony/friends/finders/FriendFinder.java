package la.moony.friends.finders;

import la.moony.friends.vo.FriendPostVo;
import la.moony.friends.vo.FriendVo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

public interface FriendFinder {

    /**
     * List all friends.
     *
     * @return a flux of friend vo.
     */
    Flux<FriendPostVo> listAll();

    Flux<FriendVo> friendListAll();

    /**
     * List friends by page.
     *
     * @param page page number.
     * @param size page size.
     * @return a mono of list result.
     */
    Mono<ListResult<FriendPostVo>> list(Integer page, Integer size);


    Mono<ListResult<FriendVo>> friendList(Integer page, Integer size);

    Mono<FriendPostVo> get(String friendPostName);

    Mono<FriendVo> friendGet(String friendName);

    Flux<FriendPostVo> listByUrl(String url);

    Flux<FriendPostVo> listByAuthor(String author);

}
