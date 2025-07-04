package cn.org.shelly.edu.model.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 游戏主表：记录游戏基本状态、元信息、阶段进度等
 * @TableName game
 */
@TableName(value ="game")
@Data
@Accessors(chain = true)
public class Game implements Serializable {
    /**
     * 游戏ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "游戏ID")
    private Long id;

    /**
     * 班号
     */
    @TableField(value = "cid")
    @Schema(description = "班号")
    private Long cid;

    /**
     * 学生总数
     */
    @TableField(value = "student_count")
    @Schema(description = "学生总数")
    private Integer studentCount;

    /**
     * 小组总数
     */
    @TableField(value = "team_count")
    @Schema(description = "小组总数")
    private Integer teamCount;

    /**
     * 每组计分人数
     */
    @TableField(value = "team_member_count")
    @Schema(description = "每组计分人数")
    private Integer teamMemberCount;

    /**
     * 当前赛段：1-棋盘赛，2-提案赛，0-未初始化阶段
     */
    @TableField(value = "stage")
    @Schema(description = "当前赛段：1-棋盘赛，2-提案赛，0-未初始化阶段")
    private Integer stage;

    /**
     * 当前棋盘赛轮次（1~4）
     */
    @TableField(value = "chess_round")
    @Schema(description = "当前棋盘赛轮次（1~4）")
    private Integer chessRound;

    /**
     * 当前棋盘阶段，0-上传成绩阶段 1-走棋，2-结算
     */
    @TableField(value = "chess_phase")
    @Schema(description = "当前棋盘阶段：0-上传成绩阶段 1-走棋，2-结算")
    private Integer chessPhase;

    /**
     * 当前提案阶段（1~3）
     */
    @TableField(value = "proposal_stage")
    @Schema(description = "提案阶段（1~3）")
    private Integer proposalStage;

    /**
     * 当前提案阶段内的轮次
     */
    @TableField(value = "proposal_round")
    @Schema(description = "前提案阶段内的轮次")
    private Integer proposalRound;

    /**
     * 游戏状态：1-进行中，2-已结束，3-已暂停
     */
    @TableField(value = "status")
    @Schema(description = "游戏状态：1-进行中，2-已结束")
    private Integer status;

    /**
     * 上次保存时间
     */
    @TableField(value = "last_saved_at")
    @Schema(description = "上次保存时间")
    private Date lastSavedAt;

    /**
     * 创建时间
     */
    @TableField(value = "gmt_create")
    @Schema(description = "创建时间")
    private Date gmtCreate;

    /**
     * 更新时间
     */
    @TableField(value = "gmt_update")
    @Schema(description = "更新时间")
    private Date gmtUpdate;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}