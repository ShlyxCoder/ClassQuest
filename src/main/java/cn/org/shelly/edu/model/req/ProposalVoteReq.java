package cn.org.shelly.edu.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ProposalVoteReq {
    @Schema(description = "游戏ID")
    private Long gameId;

    @Schema(description = "轮次")
    private Integer round;

    @Schema(description = "投票详情列表")
    private List<ProposalVoteItem> votes;
}
