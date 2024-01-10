package la.moony.friends.vo;

import la.moony.friends.extension.FriendPost;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import run.halo.app.extension.MetadataOperator;


@Data
@SuperBuilder
@ToString
@EqualsAndHashCode
public class FriendPostVo {

    private MetadataOperator metadata;

    private FriendPost.Spec spec;

    public static FriendPostVo from(FriendPost friendPost) {
        return FriendPostVo.builder()
            .metadata(friendPost.getMetadata())
            .spec(friendPost.getSpec())
            .build();
    }
}
