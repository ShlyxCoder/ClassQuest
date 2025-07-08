package cn.org.shelly.edu.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ProposalSecondSubmitReq {
    @Schema(description = "游戏ID")
    private Long gameId;

    @Schema(description = "老师指定一方数量")
    private Integer num;

    @Schema(description = "提案列表")
    private List<SecondSingleProposal> proposals;

    @Data
    public static class SecondSingleProposal {

        @Schema(description = "提案小组ID")
        private Long proposerTeamId;

        @Schema(description = "正方小组ID")
        private List<Long> proTeamIds;

        @Schema(description = "反方小组ID")
        private List<Long> conTeamIds;


    }
}
