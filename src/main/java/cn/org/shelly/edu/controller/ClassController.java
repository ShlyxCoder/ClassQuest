package cn.org.shelly.edu.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.org.shelly.edu.common.PageInfo;
import cn.org.shelly.edu.common.Result;
import cn.org.shelly.edu.exception.CustomException;
import cn.org.shelly.edu.model.dto.StudentExcelDTO;
import cn.org.shelly.edu.model.pojo.ClassStudent;
import cn.org.shelly.edu.model.pojo.Classes;
import cn.org.shelly.edu.model.req.FileUploadReq;
import cn.org.shelly.edu.model.req.UploadSingleStudentReq;
import cn.org.shelly.edu.model.resp.SingleStudentResp;
import cn.org.shelly.edu.model.resp.UploadResultResp;
import cn.org.shelly.edu.service.ClassService;
import cn.org.shelly.edu.service.ClassStudentService;
import cn.org.shelly.edu.utils.ExcelUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
@RequestMapping("/class")
@RequiredArgsConstructor
@Tag(name= "班级管理")
public class ClassController {
    private final ClassService classService;
    private final ClassStudentService classStudentService;
    @PostMapping
    @Operation(summary = "添加班级")
    public Result<?> addClass(@RequestParam @Schema(description = "班级编码（习近平思想2003）") String code){
        Long uid = StpUtil.getLoginIdAsLong();
        Classes c = new Classes().setCourseId(-1L).setClassCode(code).setTId(uid).setStatus(1).setCurrentStudents(0);
        Long count = classService.lambdaQuery()
                .eq(Classes::getClassCode,code)
                .eq(Classes::getTId,uid)
                .count();
        if(count > 0){
            return Result.fail("班级已存在");
        }
        classService.save(c);
        return Result.success();
    }
    @DeleteMapping("/{id}")
    @Operation(summary = "删除班级")
    public Result<?> delete(@PathVariable("id") Long id){
        classService.removeById(id);
        return Result.success();
    }
    @GetMapping("/{id}")
    @Operation(summary = "获取课程信息")
    public Result<Classes> get(@PathVariable("id") Long id) {
        return Result.success(classService.getById(id));
    }

    @PutMapping("/{id}/{status}")
    @Operation(summary = "修改班级状态")
    public Result<?> update(@PathVariable("id") Long id, @PathVariable("status") Integer status){
        Long uid = StpUtil.getLoginIdAsLong();
        Classes c = classService.lambdaQuery()
                .eq(Classes::getTId,uid)
                .eq(Classes::getId,id)
                .one();
        if(c == null){
            throw new CustomException("您暂时无权修改该记录！");
        }
        c.setStatus(status);
        classService.updateById(c);
        return Result.success();
    }
    @GetMapping("/list")
    @Operation(summary = "查看我的班级列表")
    public Result<PageInfo<Classes>> getList(
            @RequestParam(defaultValue = "1", required = false) Integer pageNum,
            @RequestParam(defaultValue = "10", required = false) Integer pageSize,
            @RequestParam(required = false, defaultValue = "") String key
    ){
        return Result.page(classService.lambdaQuery()
                .like(StringUtils.isNotBlank(key), Classes::getClassCode, key)
                .eq(Classes::getTId, StpUtil.getLoginIdAsLong())
                .orderByDesc(Classes::getStatus)
                .orderByDesc(Classes::getGmtModified)
                .page(new Page<>(pageNum, pageSize))
        );
    }
    @GetMapping("/template")
    @Operation(summary = "获取学生上传excel模板")
    public void downloadTemplate(HttpServletResponse response) {
        ExcelUtil.write(response,"学生导入模版", StudentExcelDTO.class,new ArrayList<StudentExcelDTO>());
    }
    @PostMapping("/upload")
    @Operation(summary = "上传学生excel")
    public Result<UploadResultResp> importExcel(@RequestBody FileUploadReq req) {
        return Result.success(classService.upload(req));
    }
    @PostMapping("/upload/single")
    @Operation(summary = "上传单个学生")
    public Result<Boolean> addStudent(@Validated @RequestBody UploadSingleStudentReq req) {
        return Result.isSuccess(classService.uploadSingle(req));
    }
//    @GetMapping("/student/{id}")
//    @Operation(summary = "获取学生信息")
//    public Result<ClassStudent> getStudent(@PathVariable Long id) {
//        return Result.success(classStudentService.getById(id));
//    }
    @DeleteMapping("/student/{id}")
    @Operation(summary = "删除学生")
    public Result<Boolean> deleteStudent(@PathVariable Long id) {
        return Result.isSuccess(classStudentService.removeById(id));
    }
    @PutMapping("/student")
    @Operation(summary = "更新学生")
    public Result<?> updateStudent(@Validated @RequestBody UploadSingleStudentReq req) {
        // 判断是否有相同学号+姓名+班级，但不是当前记录
        Long duplicateCount = classStudentService.lambdaQuery()
                .eq(ClassStudent::getSno, req.getSno())
                .eq(ClassStudent::getName, req.getName())
                .eq(ClassStudent::getCid, req.getCid())
                .ne(ClassStudent::getId, req.getId())
                .count();
        if (duplicateCount > 0) {
            return Result.fail("该学生信息已存在，请勿重复添加");
        }
        // 更新学生记录
        ClassStudent po = new ClassStudent()
                .setId(req.getId())
                .setCid(req.getCid())
                .setSno(req.getSno())
                .setName(req.getName());
        classStudentService.updateById(po);
        return Result.success();
    }
    @GetMapping("/student/list")
    @Operation(summary = "获取班级学生列表")
    public Result<PageInfo<SingleStudentResp>> listStudent(
            @RequestParam(defaultValue = "1", required = false) Integer pageNum,
            @RequestParam(defaultValue = "10", required = false) Integer pageSize,
            @RequestParam Integer cid,
            @RequestParam(required = false, defaultValue = "") String key
    ) {
        return Result.page(classStudentService.lambdaQuery()
                .like(StringUtils.isNotBlank(key), ClassStudent::getName, key)
                        .or()
                .like(StringUtils.isNotBlank(key), ClassStudent::getSno, key)
                .eq(ClassStudent::getCid, cid)
                .orderByAsc(ClassStudent::getName)
                .page(new Page<>(pageNum, pageSize))
                .convert(SingleStudentResp::toResp)
        );
    }

}
