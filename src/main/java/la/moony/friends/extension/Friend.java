package la.moony.friends.extension;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

import java.time.Instant;
import java.util.Date;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@GVK(kind = "Friend", group = "friend.moony.la",
    version = "v1alpha1", singular = "friend", plural = "friends")
public class Friend extends AbstractExtension  {


    private Spec spec;

    private Status status;

    @Data
    public static class Spec {

        private String displayName;

        private String link;

        private String logo;

        private String adminEmail;

        @Schema(requiredMode = REQUIRED)
        private String rssUrl;

        private String  description;

        private Date pullTime;

        @Schema(description = "1：同步成功，0：同步失败")
        private Integer status;

        private Boolean selfSubmitted = false;

        private Instant updateTime;

        private String ipAddress;

        @Schema(defaultValue = "APPROVED")
        private SubmittedType submittedType;

        private String reason;

        public enum SubmittedType {
            SUBMITTED,
            SYSTEM_CHECK_VALID,
            SYSTEM_CHECK_INVALID,
            APPROVED,
            REJECTED;

            public static SubmittedType from(String value) {
                for (SubmittedType submitted : SubmittedType.values()) {
                    if (submitted.name().equalsIgnoreCase(value)) {
                        return submitted;
                    }
                }
                return null;
            }

        }
    }

    @Schema(
        name = "BlogStatus"
    )
    @Data
    public static class Status {

        @Schema(defaultValue = "OK")
        private StatusType statusType;
        @Schema(defaultValue = "200")
        private Integer code;
        private Instant detectedAt = Instant.now();

        public enum StatusType {
            OK,
            TIMEOUT,
            CAN_NOT_BE_ACCESSED;

            public static Status.StatusType from(String value) {
                for (Status.StatusType status : Status.StatusType.values()) {
                    if (status.name().equalsIgnoreCase(value)) {
                        return status;
                    }
                }
                return null;
            }

        }

    }




}
