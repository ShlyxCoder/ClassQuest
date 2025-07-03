package cn.org.shelly.edu.model.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 小组格子行动记录表：记录每轮选格、触发状态、效果详情
 * @TableName team_tile_action
 */
@TableName(value ="team_tile_action")
@Data
public class TeamTileAction implements Serializable {
    /**
     * 记录ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 游戏ID
     */
    @TableField(value = "game_id")
    private Long gameId;

    /**
     * 小组ID
     */
    @TableField(value = "team_id")
    private Long teamId;

    /**
     * 第几轮
     */
    @TableField(value = "round")
    private Integer round;

    /**
     * 阶段：1=选格子，2=效果阶段
     */
    @TableField(value = "phase")
    private Integer phase;

    /**
     * 格子编号
     */
    @TableField(value = "tile_index")
    private Integer tileIndex;

    /**
     * 是否已触发效果
     */
    @TableField(value = "is_triggered")
    private Integer isTriggered;

    /**
     * 格子是否仍归小组所有，false表示已被扣除或失去
     */
    @TableField(value = "is_active")
    private Integer isActive;

    /**
     * 触发效果类型（可选）
     */
    @TableField(value = "trigger_type")
    private Integer triggerType;

    /**
     * 得分变化（可选）
     */
    @TableField(value = "value_change")
    private Integer valueChange;

    /**
     * 触发效果说明（可选）
     */
    @TableField(value = "trigger_desc")
    private String triggerDesc;

    /**
     * 记录时间
     */
    @TableField(value = "gmt_create")
    private Date gmtCreate;

    /**
     * 
     */
    @TableField(value = "gmt_update")
    private Date gmtUpdate;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}