package cn.org.shelly.edu.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class ProposalDetailResp {
    @Schema(description = "提案ID")
    private Long id;
    @Schema(description = "提案小组ID")
    private Long teamId;
    @Schema(description = "组长名字")
    private String teamName;
    @Schema(description = "参与小组ID（若为第二轮，则对半取正方反方）")
    private List<Long> involvedTeamIds;
    @Schema(description = "28分的积分分配（第一轮才需要）")
    private List<Integer> scoreDistribution;
    @Schema(description = "轮次")
    private Integer round;
    @Schema(description = "得票数")
    private Integer voteCount;
}
