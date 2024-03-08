package la.moony.friends.reconciler;

import la.moony.friends.extension.Friend;
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
public class FriendReconciler implements Reconciler<Reconciler.Request> {

    private final ExtensionClient client;

    @Override
    public Result reconcile(Request request) {
        client.fetch(Friend.class, request.name()).ifPresent(friend -> {
            var status = friend.getStatus();
            if (status == null) {
                status = new Friend.Status();
                friend.setStatus(status);
            }
            status.setObservedVersion(friend.getMetadata().getVersion() + 1);
            client.update(friend);
        });
        return Result.doNotRetry();
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        final var friend = new Friend();
        return builder
            .extension(friend)
            .workerCount(5)
            .onAddMatcher(DefaultExtensionMatcher.builder(client, friend.groupVersionKind())
                .fieldSelector(
                    FieldSelector.of(equal(Friend.REQUIRE_SYNC_ON_STARTUP_INDEX_NAME, "true"))
                )
                .build()
            )
            .build();
    }

}
