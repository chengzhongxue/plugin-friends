package la.moony.friends.endpoint;

import la.moony.friends.extension.Friend;
import la.moony.friends.finders.FriendFinder;
import la.moony.friends.util.CommonUtils;
import la.moony.friends.util.IpAddressUtils;
import la.moony.friends.vo.FriendsConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.springdoc.core.fn.builders.apiresponse.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.ConfigMap;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.infra.utils.JsonUtils;
import run.halo.app.plugin.ReactiveSettingFetcher;


@Component
@Slf4j
public class FriendRequestEndpoint implements CustomEndpoint {

    public static final String GRAVATAR_ADDRESS_SMALL_SIZE = "/avatar/%s?d=mp&s=80";

    private final ReactiveExtensionClient client;

    private final FriendFinder friendFinder;

    private final ReactiveSettingFetcher settingFetcher;

    public FriendRequestEndpoint(ReactiveExtensionClient client, FriendFinder friendFinder,
        ReactiveSettingFetcher settingFetcher) {
        this.client = client;
        this.friendFinder = friendFinder;
        this.settingFetcher = settingFetcher;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        String tag = "api.plugin.halo.run/v1alpha1/Friend";
        return SpringdocRouteBuilder.route().
            POST("plugins/plugin-friends/friendRequest", this::createFriend, (builder) -> {
                builder.operationId("CreateFriend")
                    .description("Create entry.")
                    .tag(tag)
                    .response(
                    Builder.responseBuilder().implementation(Friend.class));
            }).build();
    }

    private Mono<ServerResponse> createFriend(ServerRequest request) {
        Mono<Friend> var10000 = request.bodyToMono(Friend.class).map((friend) -> {
            friend.getSpec().setIpAddress(IpAddressUtils.getIpAddress(request));
            return friend;
        }).flatMap((friend) -> {
            return friendFinder.isFriend(friend.getSpec().getRssUrl()).flatMap((data) ->{
                if (data>0){
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "您要提交的RSS链接已存在");
                }
                return paramsValidation(friend);
            });
        });
        return var10000.flatMap(client::create).flatMap((entry) -> {
            return ServerResponse.ok().bodyValue(entry);
        });
    }

    private Mono<Friend> paramsValidation(Friend friend) {
        return Mono.just(friend).doOnNext((data) -> {
            if (StringUtils.isBlank(data.getSpec().getDisplayName())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "博客名称不能为空");
            }
            String name = data.getSpec().getDisplayName().trim();
            if (name.length() > 20) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "博客名称不能大于20个字");
            }
            // description
            if (StringUtils.isBlank(data.getSpec().getDescription())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "博客描述不能为空");
            }
            String description = data.getSpec().getDescription().trim();
            if (description.length() < 5) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "博客描述不能少于5个字");
            }
            if (description.length() > 300) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "博客描述不能大于300个字");
            }
            // rss address
            if (StringUtils.isBlank(data.getSpec().getRssUrl())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RSS 地址不能为空");
            }
            String rssAddress = data.getSpec().getRssUrl().trim();
            if (!rssAddress.startsWith("http")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RSS 地址不正确");
            }
            if (!CommonUtils.getDomain(rssAddress).contains("/")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RSS 地址不正确");
            }
            // email
            if (StringUtils.isBlank(data.getSpec().getAdminEmail())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "博主邮箱不能为空");
            }
            String adminEmail = data.getSpec().getAdminEmail().trim();
            if (!isEmailValid(adminEmail)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱格式不正确");
            }
        }).flatMap((data) -> {

            return this.client.fetch(ConfigMap.class, "plugin-friends-configMap").filter((configMap) -> {
                return !configMap.getData().isEmpty() && configMap.getData().containsKey("base");
            }).flatMap((cm) -> {
                FriendsConfig config = this.getConfig(cm);
                if (config!=null) {
                    String apiUrl = config.getGravatar().getApiUrl();
                    String apiParam = config.getGravatar().getApiParam();
                    String email = CommonUtils.md5(data.getSpec().getAdminEmail());
                    data.getSpec().setLogo(apiUrl+email+"?"+apiParam);
                }
                return Mono.just(data);
            }).switchIfEmpty(Mono.just(data));
        });
    }

    private FriendsConfig getConfig(ConfigMap cm) {
        String setRef = (String)cm.getData().get("base");
        return (FriendsConfig) JsonUtils.jsonToObject(setRef, FriendsConfig.class);
    }

    public static boolean isEmailValid(String emailAddress) {
        return EmailValidator.getInstance().isValid(emailAddress);
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("api.plugin.halo.run/v1alpha1");
    }
}
