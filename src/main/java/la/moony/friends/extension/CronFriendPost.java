package la.moony.friends.extension;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;
import java.time.Instant;


@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@GVK(
    group = "friend.moony.la",
    version = "v1alpha1",
    kind = "CronFriendPost",
    singular = "cronFriendPost",
    plural = "cronFriendPosts"
)
public class CronFriendPost extends AbstractExtension {


    private Spec spec = new Spec();

    private Status status = new Status();

    @Data
    public static class Spec {
        private String cron;
        private String timezone;
        private boolean suspend;

        @Schema(
            minimum = "0"
        )
        private int successfulRetainLimit;

    }

    @Data
    public static class Status {
        private Instant lastScheduledTimestamp;
        private Instant nextSchedulingTimestamp;

    }




}
