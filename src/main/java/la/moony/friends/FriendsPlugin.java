package la.moony.friends;

import la.moony.friends.extension.CronFriendPost;
import la.moony.friends.extension.Friend;
import la.moony.friends.extension.FriendPost;
import org.pf4j.PluginWrapper;
import org.springframework.stereotype.Component;
import run.halo.app.extension.SchemeManager;
import run.halo.app.plugin.BasePlugin;

/**
 * @author moony
 * @url https://moony.la
 * @date 2024/1/7
 */
@Component
public class FriendsPlugin extends BasePlugin {

    private final SchemeManager schemeManager;

    public FriendsPlugin(PluginWrapper wrapper, SchemeManager schemeManager) {
        super(wrapper);
        this.schemeManager = schemeManager;
    }

    @Override
    public void start() {
        schemeManager.register(Friend.class);
        schemeManager.register(FriendPost.class);
        schemeManager.register(CronFriendPost.class);
    }

    @Override
    public void stop() {
        schemeManager.unregister(schemeManager.get(Friend.class));
        schemeManager.unregister(schemeManager.get(FriendPost.class));
        schemeManager.unregister(schemeManager.get(CronFriendPost.class));
    }
}
