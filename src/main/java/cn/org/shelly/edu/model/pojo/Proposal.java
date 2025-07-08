package cn.org.shelly.edu.model.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@TableName(value ="proposal")
@Data
public class Proposal implements Serializable {
    /**
     * 主键，自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 第几轮提案
     */
    @TableField(value = "round")
    private Integer round;

    /**
     * 提出提案的小组ID
     */
    @TableField(value = "proposer_team_id")
    private Long proposerTeamId;

    /**
     * 被选举获得的积分
     */
    @TableField(value = "elected_score")
    private Integer electedScore;

    /**
     * 逗号分隔的参赛小组ID列表
     */
    @TableField(value = "involved_teams")
    private String involvedTeams;

    /**
     * 28个积分的分配方式，逗号分隔字符串，如 "10,5,13"
     */
    @TableField(value = "score_distribution")
    private String scoreDistribution;

    /**
     * 是否被选中，0=否，1=是
     */
    @TableField(value = "selected")
    private Integer selected;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private Date updateTime;

    @TableField(value = "game_id")
    private Long gameId;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}