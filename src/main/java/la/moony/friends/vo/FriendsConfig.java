package la.moony.friends.vo;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode
public class FriendsConfig {

    private String title;

    private Gravatar gravatar;

    @Data
    public static class Gravatar {
        private String apiUrl;
        private String apiParam;
    }
}
