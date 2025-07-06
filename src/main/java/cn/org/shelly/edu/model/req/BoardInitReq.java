package cn.org.shelly.edu.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "棋盘初始化请求")
public class BoardInitReq {

    @NotNull
    @Schema(description = "游戏ID", example = "1001")
    private Long gameId;

    @NotNull
    @Schema(description = "棋盘总格子数", example = "36")
    private Integer totalTiles;

    @Schema(description = "黑沼泽格子编号", example = "[2, 15]")
    private List<Integer> blackSwampTiles;

    @Schema(description = "盲盒秘境格子信息（蓝色格子）")
    private List<BlindBoxTile> blindBoxTiles;

    @Schema(description = "决斗要塞格子信息（橙色格子）")
    private List<FortressTile> fortressTiles;

    @Schema(description = "黄金中心格子编号", example = "[18]")
    private List<Integer> goldCenterTiles;

    @Schema(description = "机会宝地格子编号", example = "[30]")
    private List<Integer> opportunityTiles;

    @Data
    @Schema(description = "要塞格子定义")
    public static class FortressTile {

        @NotNull
        @Schema(description = "格子编号")
        private Integer tileId;

        @NotNull
        @Schema(description = "对抗赛类型（双音节成语0 / 成语抢答1）")
        private Integer gameType;
    }
    @Data
    @Schema(description = "盲盒格子定义")
    public static class BlindBoxTile {

        @NotNull
        @Schema(description = "格子编号")
        private Integer tileId;

        @NotNull
        @Schema(description = "事件类型（天降领地0 / 图片论述1 / 五词对抗赛2）")
        private Integer eventType;
    }
}
