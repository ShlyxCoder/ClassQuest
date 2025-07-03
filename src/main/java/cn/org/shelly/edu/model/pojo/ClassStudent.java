package cn.org.shelly.edu.model.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 
 * @TableName class_student
 */
@TableName(value ="class_student")
@Data
@Accessors(chain = true)
public class ClassStudent implements Serializable {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 学生姓名
     */
    @TableField(value = "name")
    private String name;

    /**
     * 性别(0：女 1：男，默认为1)
     */
    @TableField(value = "sex")
    private Integer sex;

    /**
     * 学校名称
     */
    @TableField(value = "university_name")
    private String universityName;

    /**
     * 所属学院
     */
    @TableField(value = "department_name")
    private String departmentName;

    /**
     * 所学专业
     */
    @TableField(value = "major")
    private String major;

    /**
     * 创建时间
     */
    @TableField(value = "gmt_create")
    private Date gmtCreate;

    /**
     * 更新时间
     */
    @TableField(value = "gmt_modified")
    private Date gmtModified;

    /**
     * 班级id
     */
    @TableField(value = "cid")
    private Long cid;

    /**
     * 逻辑删除(0：未删除 1：已删除)
     */
    @TableField(value = "Is_deleted")
    private Integer isDeleted;

    /**
     * 学号
     */
    @TableField(value = "sno")
    private String sno;

    @TableField(exist = false)@Serial
    private static final long serialVersionUID = 1L;
}