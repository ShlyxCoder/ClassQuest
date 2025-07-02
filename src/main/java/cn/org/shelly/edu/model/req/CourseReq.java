package cn.org.shelly.edu.model.req;


import cn.org.shelly.edu.model.pojo.Course;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.beans.BeanUtils;

@Data
@Accessors(chain = true)
public class CourseReq {
    /**
     *
     */
    private Long id;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 课程编码
     */
    @Schema(description = "课程编码")
    private String courseCode;

    /**
     * 课程描述
     */
    @Schema(description = "课程描述")
    private String description;

    /**
     * 教师名称
     */
    @Schema(description = "教师名称")
    private String tName;

    /**
     * 学期
     */
    @Schema(description = "学期")
    private String semester;

    /**
     * 学年
     */
    @Schema(description = "学年")
    private String academicYear;

    /**
     * 状态：1-进行中，0-已结束
     */
    @Schema(description = "状态：1-进行中，0-已结束")
    private Integer status;

    public static Course toCourse(CourseReq req) {
        return new Course()
                .setCourseName(req.getCourseName())
                .setId(req.getId())
                .setCourseCode(req.getCourseCode())
                .setDescription(req.getDescription())
                .setTName(req.getTName())
                .setSemester(req.getSemester())
                .setAcademicYear(req.getAcademicYear())
                .setStatus(req.getStatus());
    }
}
