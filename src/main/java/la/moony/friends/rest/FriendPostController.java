package la.moony.friends.rest;

import la.moony.friends.service.FriendPostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ApiVersion;

/**
 * RSS订阅接口
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

    @Autowired
    private FriendPostService friendPostService;

    @GetMapping("/synchronizationFriend")
    public Mono<Void> synchronizationFriend() {
        friendPostService.synchronizationFriend();
        return Mono.empty();
    }




}
