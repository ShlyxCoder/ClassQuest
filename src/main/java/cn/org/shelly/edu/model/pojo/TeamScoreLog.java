package cn.org.shelly.edu.model.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 小组得分日志表：记录每次得分及来源
 * @TableName team_score_log
 */
@TableName(value ="team_score_log")
@Data
public class TeamScoreLog implements Serializable {
    /**
     * 日志ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 小组ID
     */
    @TableField(value = "team_id")
    private Long teamId;

    /**
     * 所属游戏ID
     */
    @TableField(value = "game_id")
    private Long gameId;

    /**
     * 本次变动的分数
     */
    @TableField(value = "score")
    private BigDecimal score;

    /**
     * 得分原因，(1：老师加分，2：老师扣分，3：学习通导入成绩)
     */
    @TableField(value = "reason")
    @Schema(description = "得分原因，(1：老师加分，2：老师扣分，3：学习通导入成绩)")
    private Integer reason;

    /**
     * 游戏中的轮次（可选）
     */
    @TableField(value = "round")
    private Integer round;

    /**
     * 游戏阶段：1-棋盘赛，2-提案赛
     */
    @TableField(value = "phase")
    private Integer phase;

    /**
     * 得分时间
     */
    @TableField(value = "gmt_create")
    private Date gmtCreate;

    @TableField(value = "submit_time")
    private LocalDateTime submitTime;

    @TableField(value = "comment")
    private String comment;


    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
    public static TeamScoreLog createLog(Long teamId, Long gameId, BigDecimal score, Integer stage, Integer round, String comment) {
        TeamScoreLog log = new TeamScoreLog();
        log.setTeamId(teamId);
        log.setGameId(gameId);
        log.setScore(score);
        log.setReason(score.compareTo(BigDecimal.ZERO) > 0 ? 1 : 2);
        log.setPhase(stage);
        log.setRound(round);
        log.setComment(comment);
        return log;
    }
}