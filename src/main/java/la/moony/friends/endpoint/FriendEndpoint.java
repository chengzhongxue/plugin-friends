package la.moony.friends.endpoint;

import la.moony.friends.extension.Friend;
import la.moony.friends.extension.FriendPost;
import la.moony.friends.finders.FriendFinder;
import la.moony.friends.query.FriendPostQuery;
import la.moony.friends.query.FriendQuery;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.router.QueryParamBuildUtil;

@Component
public class FriendEndpoint implements CustomEndpoint {

    private final String friendTag = "api.plugin.halo.run/v1alpha1/Friend";
    private final String friendPostTag = "api.plugin.halo.run/v1alpha1/FriendPost";

    private final FriendFinder friendFinder;
    private final ReactiveExtensionClient client;

    public FriendEndpoint(FriendFinder friendFinder, ReactiveExtensionClient client) {
        this.friendFinder = friendFinder;
        this.client = client;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return SpringdocRouteBuilder.route()
            .GET("plugins/plugin-friends/friends", this::listFriend, builder -> {
                builder.operationId("listFriends")
                    .description("List friend.")
                    .tag(friendTag);
                QueryParamBuildUtil.buildParametersFromType(builder, FriendQuery.class);
            }).GET("plugins/plugin-friends/friendposts", this::listFriendPost, builder -> {
                builder.operationId("listFriendPosts")
                    .description("List friendPost.")
                    .tag(friendPostTag);
                QueryParamBuildUtil.buildParametersFromType(builder, FriendPostQuery.class);
            }).GET("plugins/plugin-friends/blogs", this::listBlogs, builder -> {
                builder.operationId("listBlogs")
                    .description("List blogs.")
                    .tag(friendPostTag);
            }).build();
    }

    Mono<ServerResponse> listFriend(ServerRequest request) {
        FriendQuery friendQuery = new FriendQuery(request.exchange());
        return listFriend(friendQuery)
            .flatMap(friends -> ServerResponse.ok().bodyValue(friends));
    }

    private Mono<ListResult<Friend>> listFriend(FriendQuery query) {
        return client.list(Friend.class, query.toPredicate(),
            query.toComparator(),
            query.getPage(),
            query.getSize()
        );
    }

    Mono<ServerResponse> listFriendPost(ServerRequest request) {
        FriendPostQuery friendPostQuery = new FriendPostQuery(request.exchange());
        return listFriendPost(friendPostQuery)
            .flatMap(friendPosts -> ServerResponse.ok().bodyValue(friendPosts));
    }

    Mono<ServerResponse> listBlogs(ServerRequest request) {
        return friendFinder.friendListAll().collectList()
            .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    private Mono<ListResult<FriendPost>> listFriendPost(FriendPostQuery query) {
        return client.list(FriendPost.class, query.toPredicate(),
            query.toComparator(),
            query.getPage(),
            query.getSize()
        );
    }


    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("api.plugin.halo.run/v1alpha1");
    }
}
