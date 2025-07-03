package cn.org.shelly.edu.model.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
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
    private Integer score;

    /**
     * 得分原因
     */
    @TableField(value = "reason")
    private String reason;

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

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}