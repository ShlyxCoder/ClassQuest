package cn.org.shelly.edu.service.impl;

import cn.org.shelly.edu.enums.CodeEnum;
import cn.org.shelly.edu.exception.CustomException;
import cn.org.shelly.edu.listener.StudentListener;
import cn.org.shelly.edu.mapper.ClassMapper;
import cn.org.shelly.edu.model.dto.StudentExcelDTO;
import cn.org.shelly.edu.model.pojo.ClassStudent;
import cn.org.shelly.edu.model.pojo.Classes;
import cn.org.shelly.edu.model.req.FileUploadReq;
import cn.org.shelly.edu.model.req.UploadSingleStudentReq;
import cn.org.shelly.edu.model.resp.UploadResultResp;
import cn.org.shelly.edu.service.ClassService;
import cn.org.shelly.edu.service.ClassStudentService;
import com.alibaba.excel.EasyExcelFactory;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
* @author Shelly6
* @description 针对表【class(班级表)】的数据库操作Service实现
* @createDate 2025-07-02 10:22:21
*/
@Service
@RequiredArgsConstructor
public class ClassServiceImpl extends ServiceImpl<ClassMapper, Classes>
    implements ClassService {
    private final ClassStudentService classStudentService;

    @Override
    public UploadResultResp upload(FileUploadReq req) {
        try {
            // 创建监听器实例
            StudentListener listener = new StudentListener(classStudentService, req.getId());
            // 执行解析
            EasyExcelFactory.read(req.getFile().getInputStream(), StudentExcelDTO.class, listener)
                    .sheet()
                    .doRead();
            // 返回统计结果
            return new UploadResultResp(listener.getSuccessCount(), listener.getFailCount());
        } catch (IOException e) {
            throw new CustomException("文件读取失败");
        }
    }

    @Override
    public Boolean uploadSingle(UploadSingleStudentReq req) {
        return classStudentService.save(new ClassStudent()
                .setName(req.getName())
                .setSno(req.getSno())
                .setCid(req.getCid()));
    }
}




