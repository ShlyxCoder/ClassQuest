package cn.org.shelly.edu.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "特殊格子信息")
public class SpecialTileResp {

    @Schema(description = "格子编号")
    private Integer tileId;

    @Schema(description = "格子类型（1 盲盒 / 2 决斗要塞 / 3黄金 / 4机会）")
    private Integer tileType;

    @Schema(description = "事件类型 （在type为1 图片论述0 / 五词对抗1） （在type为2 双音节成语0 / 成语抢答1）")
    private Integer eventType;

    @Schema(description = "事件名称")
    private String eventName;
}
