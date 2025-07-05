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
 * 班级表
 * @TableName class
 */
@TableName(value ="classes")
@Data
@Accessors(chain = true)
public class Classes implements Serializable {
    /**
     * 
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 班级编码
     */
    @TableField(value = "class_code")
    private String classCode;

    /**
     * 所属课程ID
     */
    @TableField(value = "course_id")
    private Long courseId;

    /**
     * 当前学生数
     */
    @TableField(value = "current_students")
    private Integer currentStudents;

    /**
     * 状态：1-活跃，0-归档
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 授课教师id
     */
    @TableField(value = "t_id")
    private Long tId;

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
     * 逻辑删除
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}