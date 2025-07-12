package cn.org.shelly.edu.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "小组排名信息")
public  class TeamRankResp {
    @Schema(description = "小组编号")
    private Long teamId;
    @Schema(description = "组长姓名")
    private String leaderName;
    @Schema(description = "小组总领地")
    private Integer totalTile;
    @Schema(description = "队长学号")
    private String leaderSno;
    @Schema(description = "小组总得分")
    private Integer totalScore;
    @Schema(description = "小组状态（1：正常，2：淘汰）")
    private Integer status;
}
