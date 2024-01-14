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

    @Data
    public static class Spec {
        private String displayName;

        private String logo;

        @Schema(requiredMode = REQUIRED)
        private String rssUrl;

        private String link;

        private String  description;


        private Date pullTime;

        @Schema(description = "1：同步成功，0：同步失败")
        private Integer status;
    }




}
