package cn.org.shelly.edu.service.impl;

import cn.org.shelly.edu.mapper.StudentScoreLogMapper;
import cn.org.shelly.edu.model.pojo.StudentScoreLog;
import cn.org.shelly.edu.service.StudentScoreLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
* @author Shelly6
* @description 针对表【student_score_log(个人得分日志表：记录学生每次得分及原因)】的数据库操作Service实现
* @createDate 2025-07-03 20:58:24
*/
@Service
public class StudentScoreLogServiceImpl extends ServiceImpl<StudentScoreLogMapper, StudentScoreLog>
    implements StudentScoreLogService {

}




