package la.moony.friends;

import la.moony.friends.extension.CronFriendPost;
import la.moony.friends.extension.FriendPost;
import la.moony.friends.extension.RssFeedSyncLog;
import org.springframework.stereotype.Component;
import run.halo.app.extension.Scheme;
import run.halo.app.extension.SchemeManager;
import run.halo.app.extension.index.IndexSpec;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

import static run.halo.app.extension.index.IndexAttributeFactory.simpleAttribute;

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
                .setName("spec.postLink")
                .setIndexFunc(
                    simpleAttribute(FriendPost.class, friendPost -> friendPost.getSpec().getPostLink())));
            indexSpecs.add(new IndexSpec()
                .setName("spec.authorUrl")
                .setIndexFunc(
                    simpleAttribute(FriendPost.class, friendPost -> friendPost.getSpec().getAuthorUrl())));
            indexSpecs.add(new IndexSpec()
                .setName("spec.linkName")
                .setIndexFunc(
                    simpleAttribute(FriendPost.class, friendPost -> friendPost.getSpec().getLinkName())));
            indexSpecs.add(new IndexSpec()
                .setName("spec.pubDate")
                .setIndexFunc(simpleAttribute(FriendPost.class, friendPost -> {
                    var pubDate = friendPost.getSpec().getPubDate();
                    return pubDate == null ? null : pubDate.toString();
                }))
            );
        });
        schemeManager.register(CronFriendPost.class);
        schemeManager.register(RssFeedSyncLog.class, indexSpecs -> {
            indexSpecs.add(new IndexSpec()
                .setName("linkName")
                .setIndexFunc(
                    simpleAttribute(RssFeedSyncLog.class, rssFeedSyncLog -> rssFeedSyncLog.getLinkName())));
            indexSpecs.add(new IndexSpec()
                .setName("state")
                .setIndexFunc(simpleAttribute(RssFeedSyncLog.class, rssFeedSyncLog -> {
                    var state = rssFeedSyncLog.getState();
                    return state == null ? null : state.name();
                })));
        });

    }

    @Override
    public void stop() {
        schemeManager.unregister(Scheme.buildFromType(FriendPost.class));
        schemeManager.unregister(Scheme.buildFromType(CronFriendPost.class));
        schemeManager.unregister(Scheme.buildFromType(RssFeedSyncLog.class));

    }
}
