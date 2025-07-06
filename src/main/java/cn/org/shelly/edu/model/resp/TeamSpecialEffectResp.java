package cn.org.shelly.edu.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "小组特殊格子效果列表")
public class TeamSpecialEffectResp {

    @Schema(description = "小组ID")
    private Long teamId;

    @Schema(description = "未触发的特殊格子列表")
    private List<SpecialTileResp> unTriggeredTiles;
}
