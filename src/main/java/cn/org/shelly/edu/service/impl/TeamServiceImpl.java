package cn.org.shelly.edu.service.impl;
import cn.org.shelly.edu.mapper.TeamMapper;
import cn.org.shelly.edu.model.pojo.Team;
import cn.org.shelly.edu.service.TeamService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
* @author Shelly6
* @description 针对表【team(小组表（固定分组，存储姓名和总人数）)】的数据库操作Service实现
* @createDate 2025-07-02 10:22:21
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService {

}




