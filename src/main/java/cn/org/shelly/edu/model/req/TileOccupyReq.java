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
    @Schema(description = "格子ID")
    private List<Integer> tileIds;
}
