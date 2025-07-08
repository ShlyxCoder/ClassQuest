package cn.org.shelly.edu.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ProposalCommonResp {
    @Schema(description = "提案ID")
    private Long id;
    @Schema(description = "提案小组ID")
    private Long proposerTeamId;
    @Schema(description = "组长名字")
    private String proposerTeamName;

    @Schema(description = "参与小组ID（若为第二轮，则对半取）")
    private List<Long> involvedTeamIds;

    @Schema(description = "28分的积分分配（第一轮才需要）")
    private List<Integer> scoreDistribution;
    @Schema(description = "是否被选中")
    private Boolean isSelected;

}