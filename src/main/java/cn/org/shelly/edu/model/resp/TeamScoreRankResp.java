package cn.org.shelly.edu.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeamScoreRankResp {
    @Schema(description = "小组ID")
    private Long teamId;
    @Schema(description = "小组名称")
    private String teamName;
    @Schema(description = "小组总得分")
    private Integer totalScore;
    @Schema(description = "小组最晚提交")
    private LocalDateTime lastSubmitTime;
}
