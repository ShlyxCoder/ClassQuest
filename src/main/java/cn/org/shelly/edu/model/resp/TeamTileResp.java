package cn.org.shelly.edu.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class TeamTileResp{
        @Schema(description = "小组ID")
        private Long teamId;

        @Schema(description = "该小组占有的所有格子编号（整数列表）")
        private List<Integer> occupiedTiles;
}