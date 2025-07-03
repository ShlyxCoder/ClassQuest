package cn.org.shelly.edu.model.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 游戏主表：记录游戏基本状态、元信息、阶段进度等
 * @TableName game
 */
@TableName(value ="game")
@Data
public class Game implements Serializable {
    /**
     * 游戏ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 班号
     */
    @TableField(value = "name")
    private String name;

    /**
     * 学生总数
     */
    @TableField(value = "student_count")
    private Integer studentCount;

    /**
     * 小组总数
     */
    @TableField(value = "team_count")
    private Integer teamCount;

    /**
     * 每组计分人数
     */
    @TableField(value = "team_member_count")
    private Integer teamMemberCount;

    /**
     * 当前赛段：1-棋盘赛，2-提案赛
     */
    @TableField(value = "stage")
    private Integer stage;

    /**
     * 当前棋盘赛轮次（1~4）
     */
    @TableField(value = "chess_round")
    private Integer chessRound;

    /**
     * 当前棋盘阶段：1-MOVE（走棋），2-SETTLE（结算）
     */
    @TableField(value = "chess_phase")
    private Integer chessPhase;

    /**
     * 当前提案阶段（1~3）
     */
    @TableField(value = "proposal_stage")
    private Integer proposalStage;

    /**
     * 当前提案阶段内的轮次
     */
    @TableField(value = "proposal_round")
    private Integer proposalRound;

    /**
     * 游戏状态：1-进行中，2-已结束，3-已暂停
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 上次保存时间
     */
    @TableField(value = "last_saved_at")
    private Date lastSavedAt;

    /**
     * 创建时间
     */
    @TableField(value = "gmt_create")
    private Date gmtCreate;

    /**
     * 更新时间
     */
    @TableField(value = "gmt_update")
    private Date gmtUpdate;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}