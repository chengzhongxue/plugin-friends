package la.moony.friends.reconciler;

import la.moony.friends.extension.FriendPost;
import la.moony.friends.extension.Link;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;
import run.halo.app.extension.router.selector.FieldSelector;

import static run.halo.app.extension.index.query.Queries.equal;

@Component
@RequiredArgsConstructor
public class LinkReconciler implements Reconciler<Reconciler.Request> {

    private final ExtensionClient client;

    @Override
    public Result reconcile(Request request) {
        client.fetch(Link.class, request.name())
            .ifPresent(link -> {
                if (ExtensionUtil.isDeleted(link)) {
                    var friendPostListOptions = new ListOptions();
                    friendPostListOptions.setFieldSelector(FieldSelector.of(
                        equal("spec.linkName",link.getMetadata().getName())
                    ));
                    client.listAll(FriendPost.class,friendPostListOptions, Sort.by("metadata.creationTimestamp").descending())
                        .forEach(friendPost -> client.delete(friendPost));
                }
        });
        return Result.doNotRetry();
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        return builder
            .extension(new Link())
            .build();
    }

}
