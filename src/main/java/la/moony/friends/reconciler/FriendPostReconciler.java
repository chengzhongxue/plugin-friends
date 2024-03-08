package la.moony.friends.reconciler;

import la.moony.friends.extension.FriendPost;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import run.halo.app.extension.DefaultExtensionMatcher;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;
import run.halo.app.extension.router.selector.FieldSelector;

import static run.halo.app.extension.index.query.QueryFactory.equal;

@Component
@RequiredArgsConstructor
public class FriendPostReconciler implements Reconciler<Reconciler.Request> {

    private final ExtensionClient client;

    @Override
    public Result reconcile(Request request) {
        client.fetch(FriendPost.class, request.name()).ifPresent(friendPost -> {
            var status = friendPost.getStatus();
            if (status == null) {
                status = new FriendPost.Status();
                friendPost.setStatus(status);
            }
            status.setObservedVersion(friendPost.getMetadata().getVersion() + 1);
            client.update(friendPost);
        });
        return Result.doNotRetry();
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        final var friendPost = new FriendPost();
        return builder
            .extension(friendPost)
            .workerCount(5)
            .onAddMatcher(DefaultExtensionMatcher.builder(client, friendPost.groupVersionKind())
                .fieldSelector(
                    FieldSelector.of(equal(FriendPost.REQUIRE_SYNC_ON_STARTUP_INDEX_NAME, "true"))
                )
                .build()
            )
            .build();
    }

}
