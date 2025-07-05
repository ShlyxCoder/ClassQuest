package cn.org.shelly.edu.service;
import cn.org.shelly.edu.model.pojo.Game;
import cn.org.shelly.edu.model.req.AssignReq;
import cn.org.shelly.edu.model.req.GameInitReq;
import cn.org.shelly.edu.model.req.TileOccupyReq;
import cn.org.shelly.edu.model.resp.TeamRankResp;
import cn.org.shelly.edu.model.resp.TeamScoreRankResp;
import cn.org.shelly.edu.model.resp.TeamUploadResp;
import cn.org.shelly.edu.model.resp.UnselectedTeamResp;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
* @author Shelly6
* @description 针对表【game(游戏主表：记录游戏基本状态、元信息、阶段进度等)】的数据库操作Service
* @createDate 2025-07-03 20:58:24
*/
public interface GameService extends IService<Game> {

    TeamUploadResp init(GameInitReq req);

    List<TeamScoreRankResp> upload(MultipartFile file, Long id);

    void uploadAssign(AssignReq req);

    List<UnselectedTeamResp> getUnselectedTeamsByGame(Long gameId);

    Boolean occupy(TileOccupyReq req);

    List<TeamRankResp> getTeamRank(Game game);
}
