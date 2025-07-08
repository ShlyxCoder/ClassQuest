package cn.org.shelly.edu.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class TileOccupyReq {
    @Schema(description = "游戏ID")
    private Long gameId;

    @Schema(description = "小组ID")
    private Long teamId;

    @Schema(description = "格子ID列表")
    private List<Integer> tileIds;

    @Schema(description = "是否触发天降领地")
    private Boolean triggerBlindBox;

    @Schema(description = "触发天降领地的格子ID（可多个）")
    private List<Integer> blindBoxTileIds;

    @Schema(description = "是否触发黄金中心")
    private Boolean triggerGoldCenter;

    @Schema(description = "触发黄金中心的格子ID")
    private Integer goldCenterTileId;

    @Schema(description = "是否触发机会宝地")
    private Boolean triggerChanceLand;

    @Schema(description = "触发机会宝地的格子ID")
    private Integer chanceLandTileId;

    @Schema(description = "机会宝地挑战是否成功")
    private Boolean challengeSuccess;
}
