package cn.org.shelly.edu.service.impl;
import cn.org.shelly.edu.mapper.TeamScoreLogMapper;
import cn.org.shelly.edu.model.pojo.TeamScoreLog;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
* @author Shelly6
* @description 针对表【team_score_log(小组得分日志表：记录每次得分及来源)】的数据库操作Service实现
* @createDate 2025-07-03 20:58:24
*/
@Service
public class TeamScoreLogServiceImpl extends ServiceImpl<TeamScoreLogMapper, TeamScoreLog>
    implements IService<TeamScoreLog> {

}




