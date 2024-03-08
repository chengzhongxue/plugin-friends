package la.moony.friends.extension;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;
import java.time.Instant;

@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@GVK(kind = "FriendPost", group = "friend.moony.la",
    version = "v1alpha1", singular = "friendpost", plural = "friendposts")
public class FriendPost extends AbstractExtension {

    public static final String REQUIRE_SYNC_ON_STARTUP_INDEX_NAME = "requireSyncOnStartup";

    private Spec spec;

    private Status status;

    @Data
    public static class Spec {
        private String url;

        private String author;

        private String logo;

        private String title;

        private String link;

        private String description;

        private Instant pubDate;

        private String friendName;
    }


    @Data
    public static class Status {
        private Boolean recommended = false;
        private Boolean pinned = false;
        private long observedVersion;
    }

}
