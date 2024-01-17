package la.moony.friends.rest;

import la.moony.friends.extension.Friend;
import la.moony.friends.extension.FriendPost;
import la.moony.friends.finders.FriendFinder;
import la.moony.friends.service.FriendPostService;
import la.moony.friends.vo.FriendPostVo;
import la.moony.friends.vo.FriendVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.plugin.ApiVersion;

/**
 * RSS订阅文章接口
 *
 * @author moony
 * @url https://moony.la
 * @date 2024/1/7
 */
@ApiVersion("v1alpha1")
@RequestMapping("/friendPost")
@RestController
@Slf4j
public class FriendPostController {


    private final FriendPostService friendPostService;
    private final ReactiveExtensionClient client;

    private final FriendFinder friendFinder;

    public FriendPostController(FriendPostService friendPostService, ReactiveExtensionClient client,
        FriendFinder friendFinder) {
        this.friendPostService = friendPostService;
        this.client = client;
        this.friendFinder = friendFinder;
    }

    @PostMapping("/synchronizationFriend")
    public Mono<Void> synchronizationFriend() {
        friendPostService.synchronizationFriend();
        return Mono.empty();
    }


    /**
     * 通过订阅站点link删除订阅帖子
     *
     * @param name
     * @return
     */
    @DeleteMapping("/delByLink/{name}")
    public Mono<Void> synchronizationFriend(@PathVariable("name") String name) {
        return client.get(Friend.class, name)
            .flatMap(friend -> client.list(FriendPost.class,
                    friendPost -> StringUtils.equals(friendPost.getSpec().getUrl(),
                        friend.getSpec().getLink()), null)
                .flatMap(friendPost -> client.delete(friendPost))
            .then());
    }

    @GetMapping("/listByUrl")
    public Mono<ListResult<FriendPostVo>> listByUrl(@RequestParam("page") Integer page,
        @RequestParam("size") Integer size,
        @RequestParam("url") String url) {
        return friendFinder.listByUrl(page,size,url);
    }


}
