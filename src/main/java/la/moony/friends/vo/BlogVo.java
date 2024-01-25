package la.moony.friends.vo;

import la.moony.friends.extension.Friend;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.With;
import lombok.experimental.SuperBuilder;
import run.halo.app.extension.MetadataOperator;
import java.util.List;

@Value
@Builder
public class BlogVo {

    MetadataOperator metadata;

    private Friend.Spec spec;

    private Friend.Status status;

    @With
    List<FriendPostVo> posts;

    public static BlogVo from(Friend friend) {
        return BlogVo.builder()
            .metadata(friend.getMetadata())
            .spec(friend.getSpec())
            .status(friend.getStatus())
            .posts(List.of())
            .build();
    }

}
