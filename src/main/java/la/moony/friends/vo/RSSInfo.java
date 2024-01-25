package la.moony.friends.vo;


import la.moony.friends.extension.FriendPost;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@NoArgsConstructor
public class RSSInfo {

    private String blogTitle;
    private String blogAddress;

    private String blogDescription;


    private List<FriendPost> blogPosts;

}
