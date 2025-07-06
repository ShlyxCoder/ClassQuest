package cn.org.shelly.edu.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class OpportunitySettleReq {

    @Schema(description = "游戏ID")
    private Long gameId;

    @Schema(description = "小组ID")
    private Long teamId;

    @Schema(description = "触发的机会宝地格子ID")
    private Integer tileId;

    @Schema(description = "是否成功完成任务")
    private Boolean success;

    @Schema(description = "选择的一系列格子")
    private List<Integer> rewardTileIds;

    @Schema(description = "是否触发天降领地")
    private Boolean triggerBlindBox;

    @Schema(description = "触发天降领地的格子ID（可多个）")
    private List<Integer> blindBoxTileIds;

    @Schema(description = "是否触发黄金中心")
    private Boolean triggerGoldCenter;

    @Schema(description = "触发黄金中心的格子ID")
    private Integer goldCenterTileId;
}
