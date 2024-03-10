package la.moony.friends.service.impl;

import la.moony.friends.enums.NotificationType;
import la.moony.friends.extension.Friend;
import la.moony.friends.service.BlogCrawlerService;
import la.moony.friends.service.FriendService;
import la.moony.friends.util.EmailService;
import la.moony.friends.vo.FriendsConfig;
import la.moony.friends.vo.RSSInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ConfigMap;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.router.selector.FieldSelector;
import run.halo.app.infra.utils.JsonUtils;
import java.util.List;

import static run.halo.app.extension.index.query.QueryFactory.all;
import static run.halo.app.extension.index.query.QueryFactory.equal;

@Component
public class FriendServiceImpl  implements FriendService {

    private final Logger log = LoggerFactory.getLogger(FriendServiceImpl.class);

    private final ExtensionClient client;

    private final BlogCrawlerService blogCrawlerService;

    private final EmailService emailService;

    public FriendServiceImpl(ExtensionClient client, BlogCrawlerService blogCrawlerService,
        EmailService emailService) {
        this.client = client;
        this.blogCrawlerService = blogCrawlerService;
        this.emailService = emailService;
    }

    @Override
    public void processNewRequest() {
        try {



            List<Friend> friends = client.listAll(Friend.class, new ListOptions().setFieldSelector(
                    FieldSelector.of(equal("spec.submittedType",Friend.Spec.SubmittedType.SUBMITTED.name()))),
                Sort.by("metadata.creationTimestamp").descending());
            friends.forEach(friend -> {
                String rssUrl = friend.getSpec().getRssUrl();
                log.info("start to process blog request, rssAddress: {}", friend.getSpec().getRssUrl());
                RSSInfo rssInfo = blogCrawlerService.getRSSInfoByRSSAddress(rssUrl, 10);

                if (null == rssInfo) {
                    log.error("rss info read failed, rssAddress: {}", rssUrl);
                    friend.getSpec().setSubmittedType(Friend.Spec.SubmittedType.SYSTEM_CHECK_INVALID);
                    friend.getSpec().setReason("RSS 地址不正确，抓取不到正确内容！");
                    client.update(friend);
                    emailService.sendMail(friend.getSpec().getAdminEmail() ,NotificationType.REJECTED, friend).subscribe();
                    return;
                }
                // success
                friend.getSpec().setSubmittedType(Friend.Spec.SubmittedType.SYSTEM_CHECK_VALID);
                friend.getSpec().setReason("RSS 地址正确，系统检查有效。");
                client.update(friend);
                emailService.sendMail(friend.getSpec().getAdminEmail() ,NotificationType.AUDITED, friend).subscribe();
            });

            } catch (Exception e) {
                log.error("new request process failed!", e);
        }
    }
}
