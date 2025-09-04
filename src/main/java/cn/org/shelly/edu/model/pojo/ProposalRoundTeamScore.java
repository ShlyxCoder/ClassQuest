package cn.org.shelly.edu.model.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@TableName(value ="proposal_round_team_score")
@Data
public class ProposalRoundTeamScore implements Serializable {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 游戏ID
     */
    @TableField(value = "game_id")
    private Long gameId;

    /**
     * 提案轮次（1-3）
     */
    @TableField(value = "round")
    private Integer round;

    /**
     * 子轮次（每轮内部的小对局，如1、2、3）
     */
    @TableField(value = "sub_round")
    private Integer subRound;

    /**
     * 小组ID
     */
    @TableField(value = "team_id")
    private Long teamId;

    /**
     * 该次子轮中的得分
     */
    @TableField(value = "score")
    private BigDecimal score;

    /**
     * 老师对该次表现的评语
     */
    @TableField(value = "comment")
    private String comment;

    /**
     * 最晚提交时间
     */
    @TableField(value = "deadline")
    private LocalDateTime deadline;

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

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}