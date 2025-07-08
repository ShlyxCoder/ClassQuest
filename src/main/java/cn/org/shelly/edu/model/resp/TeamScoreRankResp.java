package cn.org.shelly.edu.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeamScoreRankResp {
    @Schema(description = "小组ID")
    private Long teamId;

    @Schema(description = "组长姓名")
    private String teamName;

    @Schema(description = "本轮得分")
    private Integer thisRoundScore;

    @Schema(description = "小组内最晚提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "小组是否淘汰")
    private Boolean eliminated;
}
