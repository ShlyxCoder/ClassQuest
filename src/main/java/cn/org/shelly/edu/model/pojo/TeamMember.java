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
 * 小组成员表：支持灵活扩展、个体得分、动态统计
 * @TableName team_member
 */
@TableName(value ="team_member")
@Data
public class TeamMember implements Serializable {
    /**
     * 记录ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属小组ID
     */
    @TableField(value = "team_id")
    private Long teamId;

    /**
     * 学生ID
     */
    @TableField(value = "student_id")
    private Long studentId;

    /**
     * 学生姓名
     */
    @TableField(value = "student_name")
    private String studentName;

    /**
     * 是否为组长
     */
    @TableField(value = "is_leader")
    private Integer isLeader;

    /**
     * 个人得分
     */
    @TableField(value = "individual_score")
    private Integer individualScore;

    /**
     * 加入时间
     */
    @TableField(value = "gmt_create")
    private Date gmtCreate;

    @TableField(value = "sno")
    private String sno;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}