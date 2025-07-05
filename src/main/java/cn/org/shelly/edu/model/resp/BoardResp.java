package cn.org.shelly.edu.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class BoardResp {
    @Schema(description = "格子总数")
    private Integer totalTiles;
    @Schema(description = "黑沼泽格子编号（整数列表）")
    private List<Integer> blackSwampTiles;

    @Schema(description = "盲盒秘境格子编号（整数列表）")
    private List<Integer> blindBoxTiles;

    @Schema(description = "决斗要塞格子编号（整数列表）")
    private List<Integer> fortressTiles;

    @Schema(description = "黄金中心格子编号（整数列表）")
    private List<Integer> goldCenterTiles;

    @Schema(description = "机会宝地格子编号（整数列表）")
    private List<Integer> opportunityTiles;

    @Schema(description = "小组信息")
    private List<TeamTileRsp> teams;
    static class TeamTileRsp{
        @Schema(description = "小组ID")
        private Long teamId;

        @Schema(description = "该小组占有的所有格子编号（整数列表）")
        private List<Integer> occupiedTiles;
    }

}
