package la.moony.friends.rest;

import la.moony.friends.finders.FriendFinder;
import la.moony.friends.vo.BlogVo;
import la.moony.friends.vo.FriendPostVo;
import la.moony.friends.vo.FriendVo;
import la.moony.friends.vo.StatisticalVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.app.theme.TemplateNameResolver;
import run.halo.app.theme.router.PageUrlUtils;
import run.halo.app.theme.router.UrlContextListResult;
import java.util.Optional;
import static run.halo.app.theme.router.PageUrlUtils.totalPage;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class FriendRouter {



    private static final String SORT_PARAM = "sort";

    private static final String KEYWORD_PARAM = "keyword";

    private final FriendFinder friendFinder;

    private final TemplateNameResolver templateNameResolver;

    private final ReactiveSettingFetcher settingFetcher;

    @Bean
    RouterFunction<ServerResponse> friendTemplateRoute() {
        return RouterFunctions.route().GET("/friends",this::handlerFunction)
            .GET("/friends/page/{page:\\d+}",this::handlerFunction)
            .GET("/blogs",this::handlerBlogsDefault)
            .GET("/blogs/page/{page:\\d+}",this::handlerBlogsDefault)
            .GET("/blogs/{name:\\S+}",this::handlerBlogDefault)
            .GET("/blog-requests/add",this::handlerBlogRequestsAddDefault)
            .GET("/blog-requests",this::handlerBlogRequestsDefault)
            .GET("/blog-requests/{name:\\S+}",this::handlerBlogRequestsDetailDefault)
            .build();
    }


    private Mono<ServerResponse> handlerFunction(ServerRequest request) {
        return  templateNameResolver.resolveTemplateNameOrDefault(request.exchange(), "friends")
            .flatMap( templateName -> ServerResponse.ok().render(templateName,
                java.util.Map.of("friends", friendPostList(request),"title",getFriendTitle(),"statistical",getStatistical())));
    }

    private Mono<ServerResponse> handlerBlogsDefault(ServerRequest request) {
        return  templateNameResolver.resolveTemplateNameOrDefault(request.exchange(), "blogs")
            .flatMap( templateName -> ServerResponse.ok().render(templateName,
                java.util.Map.of("blogs",blogList(request),"statistical",getStatistical(),"title",getFriendTitle())));
    }

    private Mono<ServerResponse> handlerBlogDefault(ServerRequest request) {
        String friendName = request.pathVariable("name");
        return  templateNameResolver.resolveTemplateNameOrDefault(request.exchange(), "blog")
            .flatMap( templateName -> ServerResponse.ok().render(templateName,
                java.util.Map.of("friend",friendFinder.friendGet(friendName),"title",getFriendTitle())));
    }

    private Mono<ServerResponse> handlerBlogRequestsAddDefault(ServerRequest request) {
        return  templateNameResolver.resolveTemplateNameOrDefault(request.exchange(), "blogs-add")
            .flatMap( templateName -> ServerResponse.ok().render(templateName,
                java.util.Map.of("title",getFriendTitle())));
    }

    private Mono<ServerResponse> handlerBlogRequestsDetailDefault(ServerRequest request) {
        String friendName = request.pathVariable("name");
        return  templateNameResolver.resolveTemplateNameOrDefault(request.exchange(), "blog-requests-detail")
            .flatMap( templateName -> ServerResponse.ok().render(templateName,
                java.util.Map.of("title",getFriendTitle(),
                    "blogRequestDetail",friendFinder.friendGet(friendName))));
    }

    private Mono<ServerResponse> handlerBlogRequestsDefault(ServerRequest request) {
        return  templateNameResolver.resolveTemplateNameOrDefault(request.exchange(), "blog-requests")
            .flatMap( templateName -> ServerResponse.ok().render(templateName,
                java.util.Map.of("title",getFriendTitle(),"blogRequests",blogRequestList(request))));
    }


    Mono<StatisticalVo> getStatistical(){
        return friendFinder.statistical();
    }

    Mono<String> getFriendTitle() {
        return this.settingFetcher.get("base").map(
            setting -> setting.get("title").asText("友链朋友圈")).defaultIfEmpty(
            "友链朋友圈");
    }

    private Mono<UrlContextListResult<FriendVo>> blogRequestList(ServerRequest request) {
        String path = request.path();
        int pageNum = pageNumInPathVariable(request);
        return friendFinder.blogRequestList(pageNum, 10)
            .map(list -> new UrlContextListResult.Builder<FriendVo>()
                .listResult(list)
                .nextUrl(PageUrlUtils.nextPageUrl(path, totalPage(list)))
                .prevUrl(PageUrlUtils.prevPageUrl(path))
                .build()
            );
    }

    private Mono<UrlContextListResult<BlogVo>> blogList(ServerRequest request) {
        String path = request.path();
        String keywordVal = request.queryParam(SORT_PARAM)
            .filter(StringUtils::isNotBlank)
            .orElse(null);
        int pageNum = pageNumInPathVariable(request);
        String keyword = keywordQueryParam(request);
        String sort = sortQueryParam(request);
        return this.settingFetcher.get("base")
            .map(item -> item.get("pageSize").asInt(10))
            .defaultIfEmpty(10)
            .flatMap(pageSize -> friendFinder.blogList(pageNum, pageSize, sort,keyword)
                .map(list -> new UrlContextListResult.Builder<BlogVo>()
                    .listResult(list)
                    .nextUrl(appendKeywordParamIfPresent(
                        PageUrlUtils.nextPageUrl(path, totalPage(list)), keywordVal)
                    )
                    .prevUrl(appendKeywordParamIfPresent(PageUrlUtils.prevPageUrl(path), keywordVal))
                    .build()
                )
            );
    }


    private Mono<UrlContextListResult<FriendPostVo>> friendPostList(ServerRequest request) {
        String path = request.path();
        String keywordVal = request.queryParam(SORT_PARAM)
            .filter(StringUtils::isNotBlank)
            .orElse(null);
        int pageNum = pageNumInPathVariable(request);
        String keyword = keywordQueryParam(request);
        String sort = sortQueryParam(request);
        return this.settingFetcher.get("base")
            .map(item -> item.get("pageSize").asInt(10))
            .defaultIfEmpty(10)
            .flatMap(pageSize -> friendFinder.list(pageNum, pageSize,keyword)
                .map(list -> new UrlContextListResult.Builder<FriendPostVo>()
                    .listResult(list)
                    .nextUrl(appendKeywordParamIfPresent(
                        PageUrlUtils.nextPageUrl(path, totalPage(list)), keywordVal)
                    )
                    .prevUrl(appendKeywordParamIfPresent(PageUrlUtils.prevPageUrl(path), keywordVal))
                    .build()
                )
            );
    }

    private String keywordQueryParam(ServerRequest request) {
        return request.queryParam(KEYWORD_PARAM).orElse(null);
    }

    private String sortQueryParam(ServerRequest request) {
        return request.queryParam(SORT_PARAM).orElse(null);
    }
    String appendKeywordParamIfPresent(String uriString, String value) {
        return UriComponentsBuilder.fromUriString(uriString)
            .queryParamIfPresent(KEYWORD_PARAM, Optional.ofNullable(value))
            .build()
            .toString();
    }


    private int pageNumInPathVariable(ServerRequest request) {
        String page = request.pathVariables().get("page");
        return NumberUtils.toInt(page, 1);
    }

}
