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
 * 课程表
 * @TableName course
 */
@TableName(value ="course")
@Data
public class Course implements Serializable {
    /**
     * 
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 课程名称
     */
    @TableField(value = "course_name")
    private String courseName;

    /**
     * 课程编码
     */
    @TableField(value = "course_code")
    private String courseCode;

    /**
     * 课程描述
     */
    @TableField(value = "description")
    private String description;

    /**
     * 授课教师id
     */
    @TableField(value = "t_id")
    private Long tId;

    /**
     * 教师名称
     */
    @TableField(value = "t_name")
    private String tName;

    /**
     * 学期
     */
    @TableField(value = "semester")
    private String semester;

    /**
     * 学年
     */
    @TableField(value = "academic_year")
    private String academicYear;

    /**
     * 状态：1-进行中，0-已结束
     */
    @TableField(value = "status")
    private Integer status;

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