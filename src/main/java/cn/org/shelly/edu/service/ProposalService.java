package cn.org.shelly.edu.service;

import cn.org.shelly.edu.model.pojo.Proposal;
import cn.org.shelly.edu.model.req.*;
import cn.org.shelly.edu.model.resp.*;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
* @author Shelly6
* @description 针对表【proposal(提案表，存储每轮小组提案信息)】的数据库操作Service
* @createDate 2025-07-07 15:03:14
*/
public interface ProposalService extends IService<Proposal> {

    ProposalInitResp init(List<ProposalInitReq> req);

    void uploadOrder(ProposalOrderReq req);

    List<List<Long>> getProposalOrder(Long gameId);

    void uploadFirst(ProposalFirstSubmitReq req);

    void uploadSecond(ProposalSecondSubmitReq req);

    void uploadThird(ProposalThirdSubmitReq req);

    List<ProposalTeamStatusResp> getProposalAllOrder(Long gameId);

    VoteWinnerResp uploadVote(ProposalVoteReq req);

    VoteWinnerResp listSelected(Long game, Integer round);

    List<TeamScoreRankResp> upload(MultipartFile file, Long gameId);

    ProposalFirstSettleResp outTeam(OutTeamReq req);


    List<ProposalOutStatusResp> getFirstRank(Long gameId);

    DebateEvaluationResp evaluateDebateScore(DebateEvaluationReq req);

    List<ProposalRoundTeamScoreResp> getRoundScoreStatus(Long gameId);

    void adjustScore(ProposalRoundScoreAdjustReq req);

    List<ProposalRoundTeamScoreResp> settleThirdRoundBuzzCompetition(Long gameId);

    List<ProposalCommonResp> proposalList(Long gameId, Integer round);

    void adjustGlobalScore(ProposalScoreAdjustReq req);
}
