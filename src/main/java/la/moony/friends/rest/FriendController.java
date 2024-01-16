package la.moony.friends.rest;

import la.moony.friends.finders.FriendFinder;
import la.moony.friends.vo.StatisticalVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
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
@RequestMapping("/friend")
@RestController
@Slf4j
public class FriendController {


    private final FriendFinder friendFinder;

    public FriendController(FriendFinder friendFinder) {
        this.friendFinder = friendFinder;
    }

    /**
     * 站点统计
     *
     * @return StatisticalVo
     */
    @GetMapping("/statistical")
    public Mono<StatisticalVo> statistical() {
        return friendFinder.statistical();
    }


}
