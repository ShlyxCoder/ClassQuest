package cn.org.shelly.edu.service.impl;
import cn.org.shelly.edu.mapper.TeamTileActionMapper;
import cn.org.shelly.edu.model.pojo.TeamTileAction;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
* @author Shelly6
* @description 针对表【team_tile_action(小组格子行动记录表：记录每轮选格、触发状态、效果详情)】的数据库操作Service实现
* @createDate 2025-07-03 20:58:24
*/
@Service
public class TeamTileActionServiceImpl extends ServiceImpl<TeamTileActionMapper, TeamTileAction>
    implements IService<TeamTileAction> {

}




