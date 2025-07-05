package cn.org.shelly.edu.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UnselectedTeamResp {
    @Schema(description = "小组ID")
    private Long teamId;
    @Schema(description = "组长名称")
    private String leaderName;
    @Schema(description = "组长id")
    private Long leaderId;
    @Schema(description = "学号")
    private String sno;
    @Schema(description = "分配领地数量")
    private Integer assignCount;
}
