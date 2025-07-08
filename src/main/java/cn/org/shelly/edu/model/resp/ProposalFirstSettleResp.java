package cn.org.shelly.edu.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ProposalFirstSettleResp {
    @Schema(description = "是否触发结算")
    private Boolean triggered;

    private List<ScoreDetail> details;

    @Data
    public static class ScoreDetail {
        private Long teamId;
        private String leaderName;
        private Integer addScore;
        private Integer finalScore;
    }
}
