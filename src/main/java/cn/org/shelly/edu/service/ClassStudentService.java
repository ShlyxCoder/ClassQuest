package cn.org.shelly.edu.service;
import cn.org.shelly.edu.model.dto.StudentExcelDTO;
import cn.org.shelly.edu.model.pojo.ClassStudent;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Shelly6
* @description 针对表【class_student】的数据库操作Service
* @createDate 2025-07-02 10:22:21
*/
public interface ClassStudentService extends IService<ClassStudent> {

    Boolean createStudent(StudentExcelDTO studentExcelDTO, Long id);
}
