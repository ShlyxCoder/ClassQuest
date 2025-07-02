package cn.org.shelly.edu.model.resp;

import cn.org.shelly.edu.model.pojo.Course;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.experimental.Accessors;


@Data
@Accessors(chain = true)
public class CourseResp {
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

    public static CourseResp toResp(Course course) {
        return new CourseResp()
                .setId(course.getId())
                .setCourseName(course.getCourseName())
                .setCourseCode(course.getCourseCode())
                .setDescription(course.getDescription())
                .setTName(course.getTName())
                .setTId(course.getTId())
                .setSemester(course.getSemester())
                .setAcademicYear(course.getAcademicYear())
                .setStatus(course.getStatus());
    }
}
