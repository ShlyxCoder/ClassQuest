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
 * 棋盘配置表：记录格子总数、特殊格子
 * @TableName board_config
 */
@TableName(value ="board_config")
@Data
public class BoardConfig implements Serializable {
    /**
     *
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属游戏ID
     */
    @TableField(value = "game_id")
    private Long gameId;

    /**
     * 棋盘总格子数
     */
    @TableField(value = "total_tiles")
    private Integer totalTiles;

    /**
     * 黑沼泽格子编号（逗号分隔）
     */
    @TableField(value = "black_swamp_tiles")
    private String blackSwampTiles;

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
     *
     */
    @TableField(value = "gmt_create")
    private Date gmtCreate;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}