package cn.org.shelly.edu.service;
import cn.org.shelly.edu.model.pojo.Game;
import cn.org.shelly.edu.model.req.GameInitReq;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Shelly6
* @description 针对表【game(游戏主表：记录游戏基本状态、元信息、阶段进度等)】的数据库操作Service
* @createDate 2025-07-03 20:58:24
*/
public interface GameService extends IService<Game> {

    void init(GameInitReq req);
}
