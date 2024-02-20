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

    private Email email;

    @Data
    public static class Gravatar {
        private String apiUrl;
        private String apiParam;
    }

    @Data
    public static class Email {
        private boolean sendEmail;
        private String adminEmail;
        private String domain;
    }
}
