package cn.org.shelly.edu.model.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
     * 特殊格子列表（逗号分隔）
     */
    @TableField(value = "special_tile_ids")
    private String specialTileIds;

    /**
     * 
     */
    @TableField(value = "gmt_create")
    private Date gmtCreate;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}