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
 * 
 * @TableName permission
 */
@TableName(value ="permission")
@Data
public class Permission implements Serializable {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 权限名称
     */
    @TableField(value = "permission_name")
    private String permissionName;

    /**
     * 权限关键词(权限认证使用此字段)
     */
    @TableField(value = "key_name")
    private String keyName;

    /**
     * 路由路径
     */
    @TableField(value = "path")
    private String path;

    /**
     * 参数
     */
    @TableField(value = "perms")
    private String perms;

    /**
     * 路由组件
     */
    @TableField(value = "component")
    private String component;

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
    @TableField(value = "create_by")
    private String createBy;

    /**
     * 
     */
    @TableField(value = "update_by")
    private String updateBy;

    /**
     * 
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    /**
     * 父级权限id
     */
    @TableField(value = "parent_id")
    private Long parentId;

    /**
     * 图标
     */
    @TableField(value = "icon")
    private String icon;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}