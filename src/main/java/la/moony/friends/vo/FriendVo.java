package la.moony.friends.vo;


import la.moony.friends.extension.Friend;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import run.halo.app.extension.MetadataOperator;

@Data
@SuperBuilder
@ToString
@EqualsAndHashCode
public class FriendVo {

    private MetadataOperator metadata;

    private Friend.Spec spec;

    private Friend.Status status;

    public static FriendVo from(Friend friend) {
        return FriendVo.builder()
            .metadata(friend.getMetadata())
            .spec(friend.getSpec())
            .status(friend.getStatus())
            .build();
    }
}
