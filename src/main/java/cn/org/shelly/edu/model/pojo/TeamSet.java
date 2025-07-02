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
 * 分组合集表
 * @TableName team_set
 */
@TableName(value ="team_set")
@Data
public class TeamSet implements Serializable {
    /**
     * 分组合集ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属班级ID
     */
    @TableField(value = "class_id")
    private Long classId;

    /**
     * 分组合集名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 是否启用
     */
    @TableField(value = "enabled")
    private Integer enabled;

    /**
     * 
     */
    @TableField(value = "gmt_create")
    private Date gmtCreate;

    /**
     * 
     */
    @TableField(value = "gmt_modified")
    private Date gmtModified;

    /**
     * 
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}