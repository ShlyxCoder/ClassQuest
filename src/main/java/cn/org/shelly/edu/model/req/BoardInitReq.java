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

    @Schema(description = "黑沼泽格子编号（黑色格子，不可直接占领）", example = "[2, 15]")
    private List<Integer> blackSwampTiles;

    @Schema(description = "盲盒秘境格子编号（蓝色格子，触发随机事件）", example = "[5, 10, 20]")
    private List<Integer> blindBoxTiles;

    @Schema(description = "决斗要塞格子编号（橙色格子，可发起1v1挑战）", example = "[7, 13]")
    private List<Integer> fortressTiles;

    @Schema(description = "黄金中心格子编号（红色格子，占领得5块额外领地）", example = "[18]")
    private List<Integer> goldCenterTiles;

    @Schema(description = "机会宝地格子编号（绿色格子，第三轮后开放，触发单组挑战）", example = "[30, 33]")
    private List<Integer> opportunityTiles;
}
