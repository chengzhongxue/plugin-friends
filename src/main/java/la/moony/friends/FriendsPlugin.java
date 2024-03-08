package la.moony.friends;

import la.moony.friends.extension.CronFriendPost;
import la.moony.friends.extension.Friend;
import la.moony.friends.extension.FriendPost;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Component;
import run.halo.app.extension.SchemeManager;
import run.halo.app.extension.index.IndexSpec;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

import java.util.Optional;

import static run.halo.app.extension.index.IndexAttributeFactory.simpleAttribute;

/**
 * @author moony
 * @url https://moony.la
 * @date 2024/1/7
 */
@Component
public class FriendsPlugin extends BasePlugin {

    private final SchemeManager schemeManager;

    public FriendsPlugin(PluginContext pluginContext, SchemeManager schemeManager) {
        super(pluginContext);
        this.schemeManager = schemeManager;
    }

    @Override
    public void start() {
        schemeManager.register(Friend.class, indexSpecs -> {
            indexSpecs.add(new IndexSpec()
                .setName("spec.selfSubmitted")
                .setIndexFunc(
                    simpleAttribute(Friend.class, friend -> {
                        var selfSubmitted = friend.getSpec().getSelfSubmitted();
                        return selfSubmitted == null ? null : selfSubmitted.toString();
                    })));
            indexSpecs.add(new IndexSpec()
                .setName("spec.submittedType")
                .setIndexFunc(
                    simpleAttribute(Friend.class, friend -> friend.getSpec().getSubmittedType().name())));
            indexSpecs.add(new IndexSpec()
                .setName("status.statusType")
                .setIndexFunc(
                    simpleAttribute(Friend.class, friend -> friend.getStatus().getStatusType().name())));
            indexSpecs.add(new IndexSpec()
                .setName("spec.displayName")
                .setIndexFunc(
                    simpleAttribute(Friend.class, friend -> friend.getSpec().getDisplayName())));
            indexSpecs.add(new IndexSpec()
                .setName("spec.description")
                .setIndexFunc(
                    simpleAttribute(Friend.class, friend -> friend.getSpec().getDescription())));
            indexSpecs.add(new IndexSpec()
                .setName("spec.rssUrl")
                .setIndexFunc(
                    simpleAttribute(Friend.class, friend -> friend.getSpec().getRssUrl())));
            indexSpecs.add(new IndexSpec()
                .setName("spec.link")
                .setIndexFunc(
                    simpleAttribute(Friend.class, friend -> friend.getSpec().getLink())));
            indexSpecs.add(new IndexSpec()
                .setName("spec.status")
                .setIndexFunc(simpleAttribute(Friend.class, friend -> {
                    var status = friend.getSpec().getStatus();
                    return status == null ? null : status.toString();
                })));

            indexSpecs.add(new IndexSpec()
                .setName("spec.updateTime")
                .setIndexFunc(simpleAttribute(Friend.class, friend -> {
                    var updateTime = friend.getSpec().getUpdateTime();
                    return updateTime == null ? null : updateTime.toString();
                }))
            );

            indexSpecs.add(new IndexSpec()
                .setName(Friend.REQUIRE_SYNC_ON_STARTUP_INDEX_NAME)
                .setIndexFunc(simpleAttribute(Friend.class, friend -> {
                    var observedVersion = Optional.ofNullable(friend.getStatus())
                        .map(Friend.Status::getObservedVersion)
                        .orElse(-1L);
                    if (observedVersion < friend.getMetadata().getVersion()) {
                        return BooleanUtils.TRUE;
                    }
                    // don't care about the false case
                    return null;
                })));

        });
        schemeManager.register(FriendPost.class, indexSpecs -> {

            indexSpecs.add(new IndexSpec()
                .setName("spec.author")
                .setIndexFunc(
                    simpleAttribute(FriendPost.class, friendPost -> friendPost.getSpec().getAuthor())));
            indexSpecs.add(new IndexSpec()
                .setName("spec.title")
                .setIndexFunc(
                    simpleAttribute(FriendPost.class, friendPost -> friendPost.getSpec().getTitle())));
            indexSpecs.add(new IndexSpec()
                .setName("spec.description")
                .setIndexFunc(
                    simpleAttribute(FriendPost.class, friendPost -> friendPost.getSpec().getDescription())));
            indexSpecs.add(new IndexSpec()
                .setName("spec.link")
                .setIndexFunc(
                    simpleAttribute(FriendPost.class, friendPost -> friendPost.getSpec().getLink())));
            indexSpecs.add(new IndexSpec()
                .setName("spec.url")
                .setIndexFunc(
                    simpleAttribute(FriendPost.class, friendPost -> friendPost.getSpec().getUrl())));

            indexSpecs.add(new IndexSpec()
                .setName("spec.pubDate")
                .setIndexFunc(simpleAttribute(FriendPost.class, friendPost -> {
                    var pubDate = friendPost.getSpec().getPubDate();
                    return pubDate == null ? null : pubDate.toString();
                }))
            );

            indexSpecs.add(new IndexSpec()
                .setName(FriendPost.REQUIRE_SYNC_ON_STARTUP_INDEX_NAME)
                .setIndexFunc(simpleAttribute(FriendPost.class, friend -> {
                    var observedVersion = Optional.ofNullable(friend.getStatus())
                        .map(FriendPost.Status::getObservedVersion)
                        .orElse(-1L);
                    if (observedVersion < friend.getMetadata().getVersion()) {
                        return BooleanUtils.TRUE;
                    }
                    // don't care about the false case
                    return null;
                })));
        });
        schemeManager.register(CronFriendPost.class);
    }

    @Override
    public void stop() {
        schemeManager.unregister(schemeManager.get(Friend.class));
        schemeManager.unregister(schemeManager.get(FriendPost.class));
        schemeManager.unregister(schemeManager.get(CronFriendPost.class));
    }
}
