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
     * 棋盘赛第几轮
     */
    @TableField(value = "round")
    private Integer round;

    /**
     * 阶段：1=选格子，2=效果阶段
     */
    @TableField(value = "phase")
    private Integer phase;

    /**
     * 这一轮所有格子编号（逗号分隔）
     */
    @TableField(value = "all_tiles")
    private String allTiles;

    /**
     * 盲盒秘境格子编号（逗号分隔）
     */
    @TableField(value = "blind_box_tiles")
    private String blindBoxTiles;

    /**
     * 决斗要塞格子编号（逗号分隔）
     */
    @TableField(value = "fortress_tiles")
    private String fortressTiles;

    /**
     * 黄金中心格子编号（逗号分隔）
     */
    @TableField(value = "gold_center_tiles")
    private String goldCenterTiles;

    /**
     * 机会宝地格子编号（逗号分隔）
     */
    @TableField(value = "opportunity_tiles")
    private String opportunityTiles;

    /**
     * 该轮选的原始格子数量
     */
    @TableField(value = "original_tile_count")
    private Integer originalTileCount;

    /**
     * 该轮结算后剩余格子数量
     */
    @TableField(value = "settled_tile_count")
    private Integer settledTileCount;

    /**
     * 记录创建时间
     */
    @TableField(value = "gmt_create")
    private Date gmtCreate;

    /**
     * 记录更新时间
     */
    @TableField(value = "gmt_update")
    private Date gmtUpdate;


    @TableField(value = "selected")
    private Integer selected;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}