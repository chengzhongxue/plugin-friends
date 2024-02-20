package la.moony.friends.rest;

import la.moony.friends.enums.NotificationType;
import la.moony.friends.extension.Friend;
import la.moony.friends.finders.FriendFinder;
import la.moony.friends.util.EmailService;
import la.moony.friends.vo.FriendVo;
import la.moony.friends.vo.StatisticalVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.plugin.ApiVersion;
import java.util.Objects;

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

    private final EmailService emailService;

    private final ReactiveExtensionClient client;

    private final FriendFinder friendFinder;

    public FriendController(EmailService emailService, ReactiveExtensionClient client,
        FriendFinder friendFinder) {
        this.emailService = emailService;
        this.client = client;
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

    /**
     * 审核 发邮件
     *
     */
    @PostMapping("/Audit")
    public Mono<FriendVo> Audit(@RequestBody final Friend friend) {
        return client.fetch(Friend.class,friend.getMetadata().getName()).flatMap(friendVo -> {
            return client.update(friend).flatMap(f ->{
                Friend.Spec.SubmittedType submittedType = friendVo.getSpec().getSubmittedType();
                if (submittedType != null){
                    if (!Objects.equals(submittedType,Friend.Spec.SubmittedType.APPROVED) &&
                        !Objects.equals(submittedType,Friend.Spec.SubmittedType.SYSTEM_CHECK_VALID) ){
                        if (!Objects.equals(submittedType,friend.getSpec().getSubmittedType())){
                            if (Objects.equals(friend.getSpec().getSubmittedType(),Friend.Spec.SubmittedType.APPROVED)){
                                emailService.sendMail(friend.getSpec().getAdminEmail(),NotificationType.AUDITED, friend).subscribe();
                            }
                            if (Objects.equals(friend.getSpec().getSubmittedType(),Friend.Spec.SubmittedType.REJECTED)){
                                emailService.sendMail(friend.getSpec().getAdminEmail(),NotificationType.REJECTED, friend).subscribe();
                            }
                        }
                    }
                }
                return Mono.just(FriendVo.from(f));
            });
        });
    }

}
