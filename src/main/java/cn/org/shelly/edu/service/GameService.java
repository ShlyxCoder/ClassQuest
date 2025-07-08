package cn.org.shelly.edu.service;
import cn.org.shelly.edu.common.Result;
import cn.org.shelly.edu.model.dto.XxtStudentScoreExcelDTO;
import cn.org.shelly.edu.model.pojo.Game;
import cn.org.shelly.edu.model.pojo.Team;
import cn.org.shelly.edu.model.req.*;
import cn.org.shelly.edu.model.resp.*;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
* @author Shelly6
* @description 针对表【game(游戏主表：记录游戏基本状态、元信息、阶段进度等)】的数据库操作Service
* @createDate 2025-07-03 20:58:24
*/
public interface GameService extends IService<Game> {

    TeamUploadResp init(GameInitReq req);

    List<TeamScoreRankResp> upload(MultipartFile file, Long id);
    List<XxtStudentScoreExcelDTO> validateAndParseFile(MultipartFile file);
    Map<Long, List<XxtStudentScoreExcelDTO>> buildTeamGroupMap(List<XxtStudentScoreExcelDTO> scores, Long gameId, Long cid);

    void uploadAssign(AssignReq req);

    List<UnselectedTeamResp> getUnselectedTeamsByGame(Long gameId);

    Boolean occupy(TileOccupyReq req,Integer s) throws JsonProcessingException;

    List<TeamRankResp> getTeamRank(Game game);

    BoardResp showOccupyStatus(Long gameId);

    List<TeamSpecialEffectResp> getSpecialEffectList(Long gameId);

    void settleOpportunityTask(OpportunitySettleReq req);

    void settleFortressBattle(FortressBattleReq req) throws JsonProcessingException;

    void settleBlindBoxEvent(BlindBoxSettleReq req);

    List<TeamScoreRankResp> getStudentRank(Long id);

    void updateScore(ScoreUpdateReq req);

    List<TeamScoreRankResp> calculateAndUpdateTeamScoresAndGetRankResp(
            Map<Long, List<XxtStudentScoreExcelDTO>> groupMap,
            Game game,
            Map<Long, Team> teamMap);
}
