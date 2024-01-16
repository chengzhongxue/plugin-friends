package la.moony.friends.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatisticalVo {

    private Integer friendsNum;

    private Integer activeNum;

    private Integer articleNum;


    public static StatisticalVo empty() {
        return StatisticalVo.builder()
            .friendsNum(0)
            .activeNum(0)
            .articleNum(0)
            .build();
    }


}
