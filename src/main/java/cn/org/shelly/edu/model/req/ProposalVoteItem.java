package cn.org.shelly.edu.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ProposalVoteItem {
    @Schema(description = "小组ID")
    private Long teamId;

    @Schema(description = "投出的积分")
    private Integer score;

    @Schema(description = "提案ID")
    private Long proposalId;
}

