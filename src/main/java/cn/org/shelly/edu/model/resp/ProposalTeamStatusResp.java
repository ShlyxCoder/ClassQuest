package cn.org.shelly.edu.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class ProposalTeamStatusResp {
    @Schema(description = "小组ID")
    private Long teamId;
    @Schema(description = "小组得分")
    private Integer score;
    @Schema(description = "小组排名")
    private Integer rank;
    @Schema(description = "队长名")
    private String name;
}
