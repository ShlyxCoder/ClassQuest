package cn.org.shelly.edu.service.impl;

import cn.org.shelly.edu.mapper.GameMapper;
import cn.org.shelly.edu.model.pojo.Game;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
* @author Shelly6
* @description 针对表【game(游戏主表：记录游戏基本状态、元信息、阶段进度等)】的数据库操作Service实现
* @createDate 2025-07-03 20:58:24
*/
@Service
public class GameServiceImpl extends ServiceImpl<GameMapper, Game>
    implements IService<Game> {

}




