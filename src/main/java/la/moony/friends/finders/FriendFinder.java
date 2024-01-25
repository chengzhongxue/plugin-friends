package la.moony.friends.finders;

import la.moony.friends.vo.FriendPostVo;
import la.moony.friends.vo.FriendVo;
import la.moony.friends.vo.StatisticalVo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;
import java.util.Optional;

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

    Mono<ListResult<FriendPostVo>> list(Integer page, Integer size,String keyword);

    Mono<ListResult<FriendVo>> friendList(Integer page, Integer size);

    Mono<ListResult<FriendVo>> friendList(int pageNum, Integer pageSize, String sort, String keyword);

    /**
     *  查询未审核的博客信息
     * @param page
     * @param size
     * @return
     */
    Mono<ListResult<FriendVo>> blogRequestList(Integer page, Integer size);

    Mono<ListResult<FriendPostVo>> listByName(Integer page, Integer size,String name);

    Mono<ListResult<FriendPostVo>> listByUrl(Integer page, Integer size,String url);

    Mono<FriendPostVo> get(String friendPostName);

    Mono<FriendVo> friendGet(String friendName);

    Flux<FriendPostVo> listByUrl(String url);


    Flux<FriendPostVo> listByAuthor(String author);

    Mono<ListResult<FriendPostVo>> listByAuthor(Integer page, Integer size,String author);

    Mono<StatisticalVo> statistical();
    /**
     * rss判断是否存在
     * @param rssUrl rss链接
     * @return
     */
    Mono<Integer> isFriend(String rssUrl);
}
