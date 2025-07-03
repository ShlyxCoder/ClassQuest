package cn.org.shelly.edu.model.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 个人得分日志表：记录学生每次得分及原因
 * @TableName student_score_log
 */
@TableName(value ="student_score_log")
@Data
public class StudentScoreLog implements Serializable {
    /**
     * 日志ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 学生ID
     */
    @TableField(value = "student_id")
    private Long studentId;

    /**
     * 所属小组ID
     */
    @TableField(value = "team_id")
    private Long teamId;

    /**
     * 所属游戏ID
     */
    @TableField(value = "game_id")
    private Long gameId;

    /**
     * 本次得分
     */
    @TableField(value = "score")
    private Integer score;

    /**
     * 得分原因
     */
    @TableField(value = "reason")
    private String reason;

    /**
     * 轮次（可选）
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