package cn.org.shelly.edu.service.impl;
import cn.org.shelly.edu.mapper.CourseMapper;
import cn.org.shelly.edu.model.pojo.Course;
import cn.org.shelly.edu.service.CourseService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
* @author Shelly6
* @description 针对表【course(课程表)】的数据库操作Service实现
* @createDate 2025-07-02 10:22:21
*/
@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course>
    implements CourseService {

}




