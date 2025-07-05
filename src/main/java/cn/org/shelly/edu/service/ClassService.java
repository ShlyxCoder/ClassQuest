package cn.org.shelly.edu.service;

import cn.org.shelly.edu.model.pojo.Classes;
import cn.org.shelly.edu.model.req.FileUploadReq;
import cn.org.shelly.edu.model.req.UploadSingleStudentReq;
import cn.org.shelly.edu.model.resp.UploadResultResp;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

/**
* @author Shelly6
* @description 针对表【class(班级表)】的数据库操作Service
* @createDate 2025-07-02 10:22:21
*/
public interface ClassService extends IService<Classes> {

    UploadResultResp upload(MultipartFile file, Long id);

    Boolean uploadSingle(UploadSingleStudentReq req);
}
