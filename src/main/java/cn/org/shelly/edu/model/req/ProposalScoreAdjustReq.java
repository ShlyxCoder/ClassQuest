package cn.org.shelly.edu.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ProposalScoreAdjustReq {
    private Long gameId;
    @Schema(description = "小组ID")
    private Long teamId;
    @Schema(description = "得分")
    private Integer score;
    @Schema(description = "备注")
    private String comment;
}
