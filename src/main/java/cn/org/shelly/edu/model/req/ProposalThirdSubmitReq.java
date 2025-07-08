package cn.org.shelly.edu.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ProposalThirdSubmitReq {
    @Schema(description = "游戏ID")
    private Long gameId;

    @Schema(description = "老师指定小组数量")
    private Integer num;

    @Schema(description = "提案列表")
    private List<ProposalThirdSubmitReq.SingleProposal> proposals;

    @Data
    public static class SingleProposal {

        @Schema(description = "提案小组ID")
        private Long proposerTeamId;

        @Schema(description = "参与小组ID")
        private List<Long> involvedTeamIds;
    }
}
