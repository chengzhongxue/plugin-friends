package la.moony.friends.service.impl;

import la.moony.friends.extension.Friend;
import la.moony.friends.service.BlogCrawlerService;
import la.moony.friends.service.FriendService;
import la.moony.friends.vo.RSSInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;
import java.util.List;

@Component
public class FriendServiceImpl  implements FriendService {

    private final Logger log = LoggerFactory.getLogger(FriendServiceImpl.class);

    private final ExtensionClient client;

    private final BlogCrawlerService blogCrawlerService;

    public FriendServiceImpl(ExtensionClient client, BlogCrawlerService blogCrawlerService) {
        this.client = client;
        this.blogCrawlerService = blogCrawlerService;
    }

    @Override
    public void processNewRequest() {
        try {

            List<Friend> friends = client.list(Friend.class, friend -> {
                  if (friend.getSpec().getSubmittedType()!=null){
                      return friend.getSpec().getSubmittedType().equals(
                          Friend.Spec.SubmittedType.SUBMITTED);
                  }else {
                      return false;
                  }
                }, null);
            friends.forEach(friend -> {
                String rssUrl = friend.getSpec().getRssUrl();
                log.info("start to process blog request, rssAddress: {}", friend.getSpec().getRssUrl());
                RSSInfo rssInfo = blogCrawlerService.getRSSInfoByRSSAddress(rssUrl, 10);

                if (null == rssInfo) {
                    log.error("rss info read failed, rssAddress: {}", rssUrl);
                    friend.getSpec().setSubmittedType(Friend.Spec.SubmittedType.SYSTEM_CHECK_INVALID);
                    friend.getSpec().setReason("RSS 地址不正确，抓取不到正确内容！");
                    client.update(friend);
                    return;
                }
                // success
                friend.getSpec().setSubmittedType(Friend.Spec.SubmittedType.SYSTEM_CHECK_VALID);
                friend.getSpec().setReason("RSS 地址正确，系统检查有效。");
                client.update(friend);
            });

            } catch (Exception e) {
                log.error("new request process failed!", e);
        }
    }
}
