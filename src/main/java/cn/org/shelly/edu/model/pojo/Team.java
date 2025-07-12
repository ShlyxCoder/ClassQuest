package cn.org.shelly.edu.model.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 小组表（固定分组，存储姓名和总人数）
 * @TableName team
 */
@Data
@TableName(value = "team")
@Schema(name = "Team", description = "小组实体：存储每个小组的基本信息与各类得分")
public class Team implements Serializable {

    @TableId(value = "id")
    @Schema(description = "小组ID")
    private Long id;

    @TableField(value = "game_id")
    @Schema(description = "所属游戏ID")
    private Long gameId;

    @TableField(value = "leader_id")
    @Schema(description = "组长学生ID")
    private Long leaderId;

    @TableField(value = "leader_name")
    @Schema(description = "组长姓名")
    private String leaderName;

    @TableField(value = "sno")
    @Schema(description = "组长学号")
    private String sno;

    @TableField(value = "total_members")
    @Schema(description = "总人数")
    private Integer totalMembers;

    @TableField(value = "member_score_sum")
    @Schema(description = "棋盘赛 - 成员得分")
    private Integer memberScoreSum;

    @TableField(value = "board_score_adjusted")
    @Schema(description = "棋盘赛 - 老师手动调整分数")
    private Integer boardScoreAdjusted;

    @TableField(value = "proposal_score_imported")
    @Schema(description = "提案赛 - 系统计算总分")
    private Integer proposalScoreImported;

    @TableField(value = "proposal_score_adjusted")
    @Schema(description = "提案赛 - 老师手动调整分数")
    private Integer proposalScoreAdjusted;

    @TableField(value = "gmt_create")
    @Schema(description = "创建时间")
    private Date gmtCreate;

    @TableField(value = "gmt_update")
    @Schema(description = "更新时间")
    private Date gmtUpdate;

    @TableField(value = "alive")
    @Schema(description = "存活状态（-1：未参与 0：提案赛暂时淘汰，1：存活，2：棋盘赛淘汰）")
    private Integer alive;

    @TableField(value = "eliminated_time")
    @Schema(description = "淘汰时间")
    private Date eliminatedTime;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}
