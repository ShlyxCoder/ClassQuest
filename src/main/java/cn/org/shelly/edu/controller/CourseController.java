package cn.org.shelly.edu.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.org.shelly.edu.common.PageInfo;
import cn.org.shelly.edu.common.Result;
import cn.org.shelly.edu.model.pojo.Course;
import cn.org.shelly.edu.model.req.CourseReq;
import cn.org.shelly.edu.model.resp.CourseResp;
import cn.org.shelly.edu.service.CourseService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
@Tag(name= "课程管理(前端暂时忽略)")
public class CourseController {
    private final CourseService courseService;
    @PostMapping
    @Operation(summary = "添加课程")
    public Result<Void> addCourse(@RequestBody CourseReq req){
        Long uid = StpUtil.getLoginIdAsLong();
        Course course = CourseReq.toCourse(req);
        course.setTId(uid);
        Long count = courseService.lambdaQuery()
                .eq(Course::getCourseCode,course.getCourseCode())
                .eq(Course::getTId,uid)
                .count();
        if(count > 0){
            return Result.fail("课程已存在");
        }
        courseService.save(course);
        return Result.success();
    }
    @PutMapping
    @Operation(summary = "修改课程")
    public Result<Void> updateCourse(@RequestBody CourseReq req){
        Long uid = StpUtil.getLoginIdAsLong();
        Course course = CourseReq.toCourse(req);
        course.setTId(uid);
        Long count = courseService.lambdaQuery()
                .eq(Course::getCourseCode,course.getCourseCode())
                .eq(Course::getTId,uid)
                .count();
        if(count > 0){
            return Result.fail("课程已存在");
        }
        courseService.updateById(course);
        return Result.success();
    }
    @DeleteMapping("/{id}")
    @Operation(summary = "删除课程")
    public Result<Void> delete(@PathVariable("id") Long id){
        courseService.removeById(id);
        return Result.success();
    }
    @GetMapping("/{id}")
    @Operation(summary = "获取课程信息")
    public Result<Course> get(@PathVariable("id") Long id) {
        return Result.success(courseService.getById(id));
    }

    @GetMapping("/list")
    @Operation(summary = "获取我的课程列表")
    public Result<PageInfo<CourseResp>>list (@RequestParam(defaultValue = "1", required = false) Integer pageNum,
                                              @RequestParam(defaultValue = "10", required = false) Integer pageSize,
                                              @RequestParam(required = false, defaultValue = "") String key){
        return Result.page(courseService.lambdaQuery()
                .like(StringUtils.isNotBlank(key), Course::getCourseName, key)
                .eq(Course::getTId, StpUtil.getLoginIdAsLong())
                .orderByDesc(Course::getStatus)
                .orderByDesc(Course::getGmtModified)
                .page(new Page<>(pageNum, pageSize))
                .convert(CourseResp::toResp)
        );
    }
}
