package la.moony.friends.rest;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import la.moony.friends.extension.Friend;
import la.moony.friends.extension.FriendPost;
import la.moony.friends.finders.FriendFinder;
import la.moony.friends.vo.FriendPostVo;
import la.moony.friends.vo.StatisticalVo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.util.comparator.Comparators;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.SortResolver;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.router.IListRequest;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.app.theme.TemplateNameResolver;
import run.halo.app.theme.router.PageUrlUtils;
import org.springframework.data.domain.Sort;
import run.halo.app.theme.router.UrlContextListResult;

import static java.util.Comparator.comparing;
import static run.halo.app.extension.router.QueryParamBuildUtil.buildParametersFromType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import java.util.Map;

import static com.fasterxml.jackson.databind.type.LogicalType.Map;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static run.halo.app.theme.router.PageUrlUtils.totalPage;
import run.halo.app.extension.Extension;
import java.time.Instant;
import static run.halo.app.extension.router.selector.SelectorUtil.labelAndFieldSelectorToPredicate;

@Configuration
@RequiredArgsConstructor
public class FriendRouter {

    private final FriendFinder friendFinder;
    private final ReactiveExtensionClient client;

    private final TemplateNameResolver templateNameResolver;

    private final ReactiveSettingFetcher settingFetcher;
    private final String friendTag = "api.plugin.halo.run/v1alpha1/Friend";
    private final String friendPostTag = "api.plugin.halo.run/v1alpha1/FriendPost";

    @Bean
    RouterFunction<ServerResponse> friendTemplateRoute() {
        return RouterFunctions.route().GET("/friends",this::handlerFunction).build();
    }


    @Bean
    RouterFunction<ServerResponse> FriendRoute() {
        return SpringdocRouteBuilder.route()
            .nest(RequestPredicates.path("/apis/api.plugin.halo.run/v1alpha1/plugins/PluginFriends"),
                this::nested,
                builder -> builder.operationId("PluginFriendsEndpoints")
                    .description("Plugin Friends Endpoints").tag(friendTag)
            )
            .build();
    }

    @Bean
    RouterFunction<ServerResponse> FriendPostRoute() {
        return SpringdocRouteBuilder.route()
            .nest(RequestPredicates.path("/apis/api.plugin.halo.run/v1alpha1/plugins/PluginFriends"),
                this::friendPostNested,
                builder -> builder.operationId("PluginFriendsEndpoints")
                    .description("Plugin Friends Endpoints").tag(friendPostTag)
            )
            .build();
    }

    RouterFunction<ServerResponse> nested() {
        return SpringdocRouteBuilder.route()
            .GET("/friends", this::listFriend,
                builder -> {
                    builder.operationId("listFriends")
                        .description("Friends Friend by query parameters")
                        .tag(friendTag);
                    buildParametersFromType(builder, FriendQuery.class);
                }
            ).build();
    }

    RouterFunction<ServerResponse> friendPostNested() {
        return SpringdocRouteBuilder.route()
            .GET("/friendPosts", this::listFriendPost,
                builder -> {
                    builder.operationId("listFriendPosts")
                        .description("Friends FriendPost by query parameters")
                        .tag(friendPostTag);
                    buildParametersFromType(builder, FriendQuery.class);
                }
            ).build();
    }


    Mono<ServerResponse> listFriend(ServerRequest request) {
        FriendQuery friendQuery = new FriendQuery(request.exchange());
        return listFriend(friendQuery)
            .flatMap(friends -> ServerResponse.ok().bodyValue(friends));
    }


    Mono<ServerResponse> listFriendPost(ServerRequest request) {
        FriendPostQuery friendPostQuery = new FriendPostQuery(request.exchange());
        return listFriendPost(friendPostQuery)
            .flatMap(friendPosts -> ServerResponse.ok().bodyValue(friendPosts));
    }

    private Mono<ListResult<Friend>> listFriend(FriendQuery query) {
        return client.list(Friend.class, query.toPredicate(),
            query.toComparator(),
            query.getPage(),
            query.getSize()
        );
    }

    private Mono<ListResult<FriendPost>> listFriendPost(FriendPostQuery query) {
        return client.list(FriendPost.class, query.toPredicate(),
            query.toComparator(),
            query.getPage(),
            query.getSize()
        );
    }


    static class FriendQuery extends IListRequest.QueryListRequest {
        private final ServerWebExchange exchange;

        public FriendQuery(ServerWebExchange exchange) {
            super(exchange.getRequest().getQueryParams());
            this.exchange = exchange;
        }

        @Schema(description = "Keyword to search links under the group")
        public String getKeyword() {
            return queryParams.getFirst("keyword");
        }

        @ArraySchema(uniqueItems = true,
            arraySchema = @Schema(name = "sort",
                description = "Sort property and direction of the list result. Supported fields: "
                    + "creationTimestamp, priority"),
            schema = @Schema(description = "friend field,asc or field,desc",
                implementation = String.class,
                example = "creationTimestamp,desc"))
        public Sort getSort() {
            return SortResolver.defaultInstance.resolve(exchange);
        }


        public Predicate<Friend> toPredicate() {
            Predicate<Friend> keywordPredicate = friend -> {
                var keyword = getKeyword();
                if (StringUtils.isBlank(keyword)) {
                    return true;
                }
                String keywordToSearch = keyword.trim().toLowerCase();
                return StringUtils.containsAnyIgnoreCase(friend.getSpec().getDisplayName(),
                    keywordToSearch)
                    || StringUtils.containsAnyIgnoreCase(friend.getSpec().getDescription(),
                    keywordToSearch)
                    || StringUtils.containsAnyIgnoreCase(friend.getSpec().getRssUrl(), keywordToSearch)
                    || StringUtils.containsAnyIgnoreCase(friend.getSpec().getLink(), keywordToSearch);
            };
            Predicate<Friend> groupPredicate = friend -> {return true;};
            Predicate<Extension> labelAndFieldSelectorToPredicate =
                labelAndFieldSelectorToPredicate(getLabelSelector(), getFieldSelector());
            return groupPredicate.and(keywordPredicate).and(labelAndFieldSelectorToPredicate);
        }

        public Comparator<Friend> toComparator() {
            var sort = getSort();
            var ctOrder = sort.getOrderFor("creationTimestamp");
            List<Comparator<Friend>> comparators = new ArrayList<>();
            if (ctOrder != null) {
                Comparator<Friend> comparator =
                    comparing(link -> link.getMetadata().getCreationTimestamp());
                if (ctOrder.isDescending()) {
                    comparator = comparator.reversed();
                }
                comparators.add(comparator);
            }
            comparators.add(compareCreationTimestamp(false));
            comparators.add(compareName(true));
            return comparators.stream()
                .reduce(Comparator::thenComparing)
                .orElse(null);
        }

        public static <E extends Extension> Comparator<E> compareCreationTimestamp(boolean asc) {
            var comparator =
                Comparator.<E, Instant>comparing(e -> e.getMetadata().getCreationTimestamp());
            return asc ? comparator : comparator.reversed();
        }

        public static <E extends Extension> Comparator<E> compareName(boolean asc) {
            var comparator = Comparator.<E, String>comparing(e -> e.getMetadata().getName());
            return asc ? comparator : comparator.reversed();
        }
    }


    static class FriendPostQuery extends IListRequest.QueryListRequest {
        private final ServerWebExchange exchange;

        public FriendPostQuery(ServerWebExchange exchange) {
            super(exchange.getRequest().getQueryParams());
            this.exchange = exchange;
        }

        @Schema(description = "Keyword to search friendPost under the group")
        public String getKeyword() {
            return queryParams.getFirst("keyword");
        }

        @ArraySchema(uniqueItems = true,
            arraySchema = @Schema(name = "sort",
                description = "Sort property and direction of the list result. Supported fields: "
                    + "creationTimestamp, priority"),
            schema = @Schema(description = "friend field,asc or field,desc",
                implementation = String.class,
                example = "creationTimestamp,desc"))
        public Sort getSort() {
            return SortResolver.defaultInstance.resolve(exchange);
        }


        public Predicate<FriendPost> toPredicate() {
            Predicate<FriendPost> keywordPredicate = friend -> {
                var keyword = getKeyword();
                if (StringUtils.isBlank(keyword)) {
                    return true;
                }
                String keywordToSearch = keyword.trim().toLowerCase();
                return StringUtils.containsAnyIgnoreCase(friend.getSpec().getAuthor(),
                    keywordToSearch)
                    || StringUtils.containsAnyIgnoreCase(friend.getSpec().getTitle(),
                    keywordToSearch)
                    || StringUtils.containsAnyIgnoreCase(friend.getSpec().getDescription(),
                    keywordToSearch)
                    || StringUtils.containsAnyIgnoreCase(friend.getSpec().getLink(), keywordToSearch);
            };
            Predicate<FriendPost> groupPredicate = friendPost -> {return true;};
            Predicate<Extension> labelAndFieldSelectorToPredicate =
                labelAndFieldSelectorToPredicate(getLabelSelector(), getFieldSelector());
            return groupPredicate.and(keywordPredicate).and(labelAndFieldSelectorToPredicate);
        }

        public Comparator<FriendPost> toComparator() {
            List<Comparator<FriendPost>> comparators = new ArrayList<>();
            // var sort = getSort();
            // var ctOrder = sort.getOrderFor("pubDate");
            // if (ctOrder != null) {
            //     Comparator<FriendPost> comparator =
            //         comparing(friendPost -> friendPost.getSpec().getPubDate());
            //     if (ctOrder.isDescending()) {
            //         comparator = comparator.reversed();
            //     }
            //     comparators.add(comparator);
            // }
            Comparator<FriendPost> comparator =
                comparing(friendPost -> friendPost.getSpec().getPubDate());
            comparators.add(comparator.reversed());
            return comparators.stream()
                .reduce(Comparator::thenComparing)
                .orElse(null);
        }


        public static <E extends Extension> Comparator<E> compareName(boolean asc) {
            var comparator = Comparator.<E, String>comparing(e -> e.getMetadata().getName());
            return asc ? comparator : comparator.reversed();
        }
    }

    private Mono<ServerResponse> handlerFunction(ServerRequest request) {
        return  templateNameResolver.resolveTemplateNameOrDefault(request.exchange(), "friends")
            .flatMap( templateName -> ServerResponse.ok().render(templateName,
                java.util.Map.of("friends", friendList(request),"title",getFriendTitle(),"statistical",getStatistical())));
    }

    Mono<StatisticalVo> getStatistical(){
        return friendFinder.statistical();
    }

    Mono<String> getFriendTitle() {
        return this.settingFetcher.get("base").map(
            setting -> setting.get("title").asText("友链朋友圈")).defaultIfEmpty(
            "友链朋友圈");
    }


    private Mono<UrlContextListResult<FriendPostVo>> friendList(ServerRequest request) {
        String path = request.path();
        int pageNum = pageNumInPathVariable(request);
        return this.settingFetcher.get("base")
            .map(item -> item.get("pageSize").asInt(10))
            .defaultIfEmpty(10)
            .flatMap(pageSize -> friendFinder.list(pageNum, pageSize)
                .map(list -> new UrlContextListResult.Builder<FriendPostVo>()
                    .listResult(list)
                    .nextUrl(UriComponentsBuilder.
                        fromUriString(PageUrlUtils.nextPageUrl(path, totalPage(list)))
                        .build()
                        .toString()
                    )
                    .prevUrl(UriComponentsBuilder.
                        fromUriString(PageUrlUtils.prevPageUrl(path))
                        .build()
                        .toString())
                    .build()
                )
            );
    }

    private int pageNumInPathVariable(ServerRequest request) {
        String page = request.pathVariables().get("page");
        return NumberUtils.toInt(page, 1);
    }

}
