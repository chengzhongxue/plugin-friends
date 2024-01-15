package la.moony.friends.extension;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;
import java.time.Instant;
import java.util.Date;

@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@GVK(kind = "FriendPost", group = "friend.moony.la",
    version = "v1alpha1", singular = "friendPost", plural = "friendPosts")
public class FriendPost extends AbstractExtension {


    private Spec spec;

    @Data
    public static class Spec {
        private String url;

        private String author;

        private String logo;

        private String title;

        private String link;

        private String description;

        private Instant pubDate;


    }

}
