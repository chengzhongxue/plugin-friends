package la.moony.friends.service.impl;


import la.moony.friends.extension.Friend;
import la.moony.friends.finders.FriendFinder;
import la.moony.friends.service.FriendPostService;
import la.moony.friends.util.RSSParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ListResult;
import java.util.function.Predicate;

@Component
@Slf4j
public class FriendPostServiceImpl implements FriendPostService {


    private final ExtensionClient client;

    private final FriendFinder friendFinder;

    private final int pageSize = 20;

    public FriendPostServiceImpl(FriendFinder friendFinder ,ExtensionClient client) {
        this.client = client;
        this.friendFinder = friendFinder;
    }


    @Override
    public void synchronizationFriend() {
        RSSParser rssParser = new RSSParser();

        Predicate<Friend> paramPredicate = post -> true;

        ListResult<Friend> listResult = client.list(Friend.class, paramPredicate, null, 1, pageSize);


        //分页导出数据
        //分页获取并处理
        for (int i = 1; i <= listResult.getTotalPages(); i++) {
            ListResult<Friend> friends = client.list(Friend.class, paramPredicate, null, i, pageSize);
            // friends.getItems().forEach(friend -> {
            //     log.info(friend.toString());
            // });
        }

        // List<Friend> listResult = client.list(Friend.class, paramPredicate, defaultFriendComparator());
        // listResult.forEach(friend -> {
        //     log.info(friend.toString());
        // });


        // friends.forEach(friend -> {
        //     try {
        //         Map<String, Object> data = rssParser.data(friend.getSpec().getRssUrl());
        //         List<FriendPost> friendPostList = (List<FriendPost>) data.get("friendPostList");
        //         log.info("friendPostList："+friendPostList.toString());
        //         friendPostList.forEach(post -> {
        //             // 设置元数据才能保存
        //             post.setMetadata(new Metadata());
        //             post.getMetadata().setGenerateName("friendPost-");
        //             post.getMetadata().setName(LocalDateTime.now().toString());
        //             FriendPost friendPost = new FriendPost();
        //             friendPost.setMetadata(new Metadata());
        //             friendPost.getMetadata().setGenerateName("friendPost-");
        //             friendPost.setSpec(post.getSpec());
        //             client.create(friendPost);
        //         });
        //     } catch (Exception e) {
        //         throw new RuntimeException(e);
        //     }
        // });
        // try {
        //     Map<String, Object> data = rssParser.data("https://moony.la/rss.xml");
        //     List<FriendPost> friendPostList = (List<FriendPost>) data.get("friendPostList");
        //     friendPostList.forEach(post -> {
        //         //设置元数据才能保存
        //         post.setMetadata(new Metadata());
        //         post.getMetadata().setGenerateName("friendPost-");
        //         post.getMetadata().setName(LocalDateTime.now().toString());
        //         FriendPost friendPost = new FriendPost();
        //         friendPost.setMetadata(new Metadata());
        //         friendPost.getMetadata().setGenerateName("friendPost-");
        //         friendPost.setSpec(post.getSpec());
        //         client.create(friendPost);
        //     });
        // } catch (Exception e) {
        //     throw new RuntimeException(e);
        // }


    }
}
