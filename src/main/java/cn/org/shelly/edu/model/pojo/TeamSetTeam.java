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
 * 分组合集与小组的关联表
 * @TableName team_set_team
 */
@TableName(value ="team_set_team")
@Data
public class TeamSetTeam implements Serializable {
    /**
     * 
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 分组合集ID
     */
    @TableField(value = "team_set_id")
    private Long teamSetId;

    /**
     * 小组ID
     */
    @TableField(value = "team_id")
    private Long teamId;

    /**
     * 
     */
    @TableField(value = "created_at")
    private Date createdAt;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}