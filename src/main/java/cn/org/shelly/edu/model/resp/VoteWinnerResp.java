package cn.org.shelly.edu.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class VoteWinnerResp {
    @Schema(description = "小组ID")
    private Long teamId;
    @Schema(description = "组长名称")
    private String leaderName;
    @Schema(description = "提案得分")
    private Integer score;
    @Schema(description = "提案ID")
    private Long proposalId;
    @Schema(description = "积分分配(若有，仅限第一轮)")
    private List<Integer> allocations;
    @Schema(description = "参赛小组")
    private List<Integer> team;
    @Schema(description = "轮次")
    private Integer round;
}
