package cn.org.shelly.edu.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class BlindBoxSettleReq {
    private Long gameId;
    private Long teamId;
    @Schema(description = "触發的蓝色格子ID")
    private Integer tileId;         // 触发的蓝色格子ID
    @Schema(description = "触发事件类型 图片论述0 / 五词对抗1")
    private Integer eventType;
    private List<Long> involvedTeamIds;
    @Schema(description = "获胜id")
    private Long winnerTeamId;
}
