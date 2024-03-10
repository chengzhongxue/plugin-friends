package la.moony.friends.service.impl;

import la.moony.friends.extension.Friend;
import la.moony.friends.service.BlogStatusService;
import la.moony.friends.util.OkHttpUtil;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.router.selector.FieldSelector;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.List;

import static run.halo.app.extension.index.query.QueryFactory.all;

@Component
public class BlogStatusServiceImpl implements BlogStatusService {
    private static final Logger log = LoggerFactory.getLogger(BlogStatusServiceImpl.class);

    private final ExtensionClient client;

    private static final OkHttpClient okHttpClient = OkHttpUtil.getUnsafeOkHttpClient();

    public BlogStatusServiceImpl(ExtensionClient client) {
        this.client = client;
    }


    @Override
    public void detectBlogStatus() {
        List<Friend> friends = client.listAll(Friend.class, new ListOptions().setFieldSelector(
            FieldSelector.of(all())), Sort.by("metadata.creationTimestamp").descending());
        friends.forEach(friend -> {
            int code = HttpStatus.OK.value();
            Friend.Status.StatusType currentStatus = Friend.Status.StatusType.OK;
            int maxReasonLength = 200;
            String link = friend.getSpec().getLink();
            if(StringUtils.isNotEmpty(link)){
                try (Response response = requestBlogAddress(friend.getSpec().getLink());
                     ResponseBody responseBody = response.body()) {

                    code = response.code();
                    String responseBodyString = responseBody.string();
                    if (HttpStatus.OK.value() != code) {
                        currentStatus = Friend.Status.StatusType.CAN_NOT_BE_ACCESSED;
                    }
                } catch (SocketTimeoutException e) {
                    log.error("timeout", e);
                    currentStatus = Friend.Status.StatusType.TIMEOUT;
                    code = HttpStatus.GATEWAY_TIMEOUT.value();
                } catch (Exception e) {
                    log.error("error in detect blog status", e);
                    currentStatus = Friend.Status.StatusType.CAN_NOT_BE_ACCESSED;
                    code = HttpStatus.INTERNAL_SERVER_ERROR.value();
                } finally {
                    if (null == friend.getStatus()) {
                        friend.setStatus(new Friend.Status());
                        friend.getStatus().setStatusType(currentStatus);
                        friend.getStatus().setCode(code);
                        friend.getStatus().setDetectedAt(Instant.now());
                        client.update(friend);
                    }else {
                        if (!currentStatus.equals(friend.getStatus().getStatusType())){
                            friend.getStatus().setStatusType(currentStatus);
                            friend.getStatus().setCode(code);
                            friend.getStatus().setDetectedAt(Instant.now());
                            client.update(friend);
                        }
                    }
                }
            }

        });

    }

    private Response requestBlogAddress(String blogAddress) throws IOException {
        Request request = new Request.Builder()
            .url(blogAddress)
            .build();

        Call call = okHttpClient.newCall(request);
        return call.execute();
    }
}
