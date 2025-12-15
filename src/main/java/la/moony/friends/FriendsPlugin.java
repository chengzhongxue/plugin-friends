package la.moony.friends;

import la.moony.friends.extension.CronFriendPost;
import la.moony.friends.extension.FriendPost;
import la.moony.friends.extension.RssFeedSyncLog;
import org.springframework.stereotype.Component;
import run.halo.app.extension.Scheme;
import run.halo.app.extension.SchemeManager;
import run.halo.app.extension.index.IndexSpecs;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

import java.time.Instant;
import java.util.Optional;

@Component
public class FriendsPlugin extends BasePlugin {

    private final SchemeManager schemeManager;

    public FriendsPlugin(PluginContext pluginContext, SchemeManager schemeManager) {
        super(pluginContext);
        this.schemeManager = schemeManager;
    }

    @Override
    public void start() {
        schemeManager.register(FriendPost.class, indexSpecs -> {
            indexSpecs.add(IndexSpecs.<FriendPost, String>single("spec.author", String.class)
                .indexFunc(
                    friendPost -> Optional.ofNullable(friendPost.getSpec())
                        .map(FriendPost.FriendPostSpec::getAuthor)
                        .orElse(null)
                )
            );
            indexSpecs.add(IndexSpecs.<FriendPost, String>single("spec.title", String.class)
                .indexFunc(
                    friendPost -> Optional.ofNullable(friendPost.getSpec())
                        .map(FriendPost.FriendPostSpec::getTitle)
                        .orElse(null)
                )
            );
            indexSpecs.add(IndexSpecs.<FriendPost, String>single("spec.description", String.class)
                .indexFunc(
                    friendPost -> Optional.ofNullable(friendPost.getSpec())
                        .map(FriendPost.FriendPostSpec::getDescription)
                        .orElse(null)
                )
            );
            indexSpecs.add(IndexSpecs.<FriendPost, String>single("spec.postLink", String.class)
                .indexFunc(
                    friendPost -> Optional.ofNullable(friendPost.getSpec())
                        .map(FriendPost.FriendPostSpec::getPostLink)
                        .orElse(null)
                )
            );
            indexSpecs.add(IndexSpecs.<FriendPost, String>single("spec.authorUrl", String.class)
                .indexFunc(
                    friendPost -> Optional.ofNullable(friendPost.getSpec())
                        .map(FriendPost.FriendPostSpec::getAuthorUrl)
                        .orElse(null)
                )
            );
            indexSpecs.add(IndexSpecs.<FriendPost, String>single("spec.linkName", String.class)
                .indexFunc(
                    friendPost -> Optional.ofNullable(friendPost.getSpec())
                        .map(FriendPost.FriendPostSpec::getLinkName)
                        .orElse(null)
                )
            );
            indexSpecs.add(IndexSpecs.<FriendPost, Instant>single("spec.pubDate", Instant.class)
                .indexFunc(
                    friendPost -> Optional.ofNullable(friendPost.getSpec())
                        .map(FriendPost.FriendPostSpec::getPubDate)
                        .orElse(null)
                )
            );
        });
        schemeManager.register(CronFriendPost.class);
        schemeManager.register(RssFeedSyncLog.class, indexSpecs -> {
            indexSpecs.add(IndexSpecs.<RssFeedSyncLog, String>single("linkName", String.class)
                .indexFunc(
                    rssFeedSyncLog -> Optional.ofNullable(rssFeedSyncLog.getLinkName())
                        .orElse(null)
                )
            );
            indexSpecs.add(IndexSpecs.<RssFeedSyncLog, RssFeedSyncLog.RssFeedSyncLogState>single("state", RssFeedSyncLog.RssFeedSyncLogState.class)
                .indexFunc(
                    rssFeedSyncLog -> Optional.ofNullable(rssFeedSyncLog.getState())
                        .orElse(null)
                )
            );
        });

    }

    @Override
    public void stop() {
        schemeManager.unregister(Scheme.buildFromType(FriendPost.class));
        schemeManager.unregister(Scheme.buildFromType(CronFriendPost.class));
        schemeManager.unregister(Scheme.buildFromType(RssFeedSyncLog.class));

    }
}
