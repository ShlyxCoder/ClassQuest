package cn.org.shelly.edu.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "积分变更请求")
public class ScoreUpdateReq {

    @Schema(description = "操作类型（1：小组 2：个人）", required = true)
    private Integer type;

    @Schema(description = "操作阶段（1：棋盘赛 2：提案赛）", required = true)
    private Integer stage;

    @Schema(description = "操作对象ID（小组ID或学生ID）", required = true)
    private Long id;

    @Schema(description = "游戏ID", required = true)
    private Long gameId;

    @Schema(description = "积分变化值", required = true)
    private Integer num;

    @Schema(description = "积分变更原因或评论", required = true)
    private String comment;
}
