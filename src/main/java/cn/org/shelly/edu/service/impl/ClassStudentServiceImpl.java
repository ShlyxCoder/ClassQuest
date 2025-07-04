package cn.org.shelly.edu.service.impl;

import cn.org.shelly.edu.mapper.ClassStudentMapper;
import cn.org.shelly.edu.model.dto.StudentExcelDTO;
import cn.org.shelly.edu.model.pojo.ClassStudent;
import cn.org.shelly.edu.service.ClassStudentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
* @author Shelly6
* @description 针对表【class_student】的数据库操作Service实现
* @createDate 2025-07-02 10:22:21
*/
@Service
@Slf4j
public class ClassStudentServiceImpl extends ServiceImpl<ClassStudentMapper, ClassStudent>
    implements ClassStudentService {

    @Override
    public Boolean createStudent(StudentExcelDTO studentExcelDTO, Long id) {
        ClassStudent classStudent = new ClassStudent();
        if(StringUtils.isNotBlank(studentExcelDTO.getSno()) || StringUtils.isNotBlank(studentExcelDTO.getName())){
            log.info("学号或姓名不能为空");
            return false;
        }
        classStudent.setName(studentExcelDTO.getName());
        classStudent.setSno(studentExcelDTO.getSno());
        classStudent.setCid(id);
        return save(classStudent);
    }
}




