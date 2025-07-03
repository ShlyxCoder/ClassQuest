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
 * 小组表（固定分组，存储姓名和总人数）
 * @TableName team
 */
@TableName(value ="team")
@Data
public class Team implements Serializable {
    /**
     * 小组ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属游戏ID
     */
    @TableField(value = "game_id")
    private Long gameId;

    /**
     * 组长学生ID
     */
    @TableField(value = "leader_id")
    private Long leaderId;

    /**
     * 组长姓名
     */
    @TableField(value = "leader_name")
    private String leaderName;

    /**
     * 总人数
     */
    @TableField(value = "total_members")
    private Integer totalMembers;

    /**
     * 小组总分
     */
    @TableField(value = "total_score")
    private Integer totalScore;

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

    @TableField(value = "alive")
    private Integer alive;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}