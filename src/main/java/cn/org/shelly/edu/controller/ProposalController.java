package cn.org.shelly.edu.controller;

import cn.org.shelly.edu.common.Result;
import cn.org.shelly.edu.exception.CustomException;
import cn.org.shelly.edu.model.req.*;
import cn.org.shelly.edu.model.resp.*;
import cn.org.shelly.edu.service.ProposalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import cn.org.shelly.edu.model.req.DebateEvaluationReq;

import java.util.List;


@RestController
@Tag(name= "提案赛管理")
@RequiredArgsConstructor
@RequestMapping("/proposal")
public class ProposalController {
    private final ProposalService proposalService;

    @GetMapping("/init/list")
    @Operation(summary = "查询提案赛真淘汰小组列表")
    public Result<List<Long>> teamList(@RequestParam Long gameId,
                                   @RequestParam @Schema(description = "1升序 2积分序") Integer sort) {
        if(sort <1 || sort >2){
            throw new CustomException("排序参数错误");
        }
        return Result.success(proposalService.scoreList(gameId,  sort));
    }
    @PostMapping("/init")
    @Operation(summary = "初始化提案赛")
    public Result<ProposalInitResp> init(@RequestBody List<ProposalInitReq> req){
        return Result.success(proposalService.init(req));
    }
    @PostMapping("/order")
    @Operation(summary = "上传提案赛选择轮次")
    public Result<Void> uploadOrder(@RequestBody @Validated ProposalOrderReq req) {
        proposalService.uploadOrder(req);
        return Result.success();
    }
    @GetMapping("/order")
    @Operation(summary = "查询提案赛每轮小组顺序")
    public Result<List<List<Long>>> getProposalOrder(@RequestParam Long gameId) {
        return Result.success(proposalService.getProposalOrder(gameId));
    }

    @PostMapping("/upload/first")
    @Operation(summary = "上传第一轮提案")
    public Result<Void> upload1(@RequestBody ProposalFirstSubmitReq req){
        proposalService.uploadFirst(req);
        return Result.success();
    }
    @PostMapping("/upload/second")
    @Operation(summary = "上传第二轮提案")
    public Result<Void> upload2(@RequestBody ProposalSecondSubmitReq req){
        proposalService.uploadSecond(req);
        return Result.success();
    }
    @PostMapping("/upload/third")
    @Operation(summary = "上传第三轮提案")
    public Result<Void> upload3(@RequestBody ProposalThirdSubmitReq req){
        proposalService.uploadThird(req);
        return Result.success();
    }
    @PostMapping("/vote")
    @Operation(summary = "上传提案赛投票")
    public Result<VoteWinnerResp> uploadVote(@RequestBody ProposalVoteReq req){
        return Result.success(proposalService.uploadVote(req));
    }
    @PostMapping("/upload/first/xxt")
    @Operation(summary = "上传提案赛学习通成绩")
    public Result<List<TeamScoreRankResp>> uploadProposalResult(@RequestPart("file") MultipartFile file,
                                                             @RequestParam Long gameId) {
        return Result.success(proposalService.upload(file, gameId));
    }
    @GetMapping("/selected/{game}/{round}")
    @Operation(summary = "查询某轮提案赛选中提案")
    public Result<VoteWinnerResp> listSelected(@PathVariable Long game, @PathVariable Integer round) {
        return Result.success(proposalService.listSelected(game, round));
    }
    @GetMapping("/list")
    @Operation(summary = "查询提案赛某轮的提案")
    public Result<List<ProposalCommonResp>> list(@RequestParam Long gameId, @RequestParam Integer round) {
        return Result.success(proposalService.proposalList(gameId, round));
    }

    @PostMapping("/outTeam")
    @Operation(summary = "淘汰小组/淘汰赛结算", description = "仅剩一个小组时自动结算")
    public Result<ProposalFirstSettleResp> outTeam(@RequestBody OutTeamReq req) {
        return Result.success(proposalService.outTeam(req));
    }
    @GetMapping("/rank/all")
    @Operation(summary = "查询所有小组提案赛总排名")
    public Result<List<ProposalTeamStatusResp>> listStatus(@RequestParam Long gameId) {
        return Result.success(proposalService.getProposalAllOrder(gameId));
    }
    @GetMapping("/rank/first")
    @Operation(summary = "查询第一轮次内队伍排名及其状态")
    public Result<List<ProposalOutStatusResp>> listRoundStatus(@RequestParam Long gameId) {
        return Result.success(proposalService.getFirstRank(gameId));
    }

    @PostMapping("/debate/evaluate")
    @Operation(summary = "辩论赛打分")
    public Result<DebateEvaluationResp> evaluateDebate(@RequestBody DebateEvaluationReq req) {
        return Result.success(proposalService.evaluateDebateScore(req));
    }
    @GetMapping("/rank/third")
    @Operation(summary = "查询第三轮次内队伍排名及其状态")
    public Result<List<ProposalRoundTeamScoreResp>> getRoundScoreStatus(
            @RequestParam Long gameId) {
        List<ProposalRoundTeamScoreResp> list = proposalService.getRoundScoreStatus(gameId);
        return Result.success(list);
    }
    @PostMapping("/buzzed")
    @Operation(summary = "抢答赛上传分数修正")
    public Result<Void> adjustScore(@RequestBody @Validated ProposalRoundScoreAdjustReq req) {
        proposalService.adjustScore(req);
        return Result.success();
    }

    @GetMapping("/settle")
    @Operation(summary = "结算第三轮抢答赛")
    public Result<List<ProposalRoundTeamScoreResp>> settleThirdRoundBuzzCompetition(@RequestParam Long gameId) {
        return Result.success(proposalService.settleThirdRoundBuzzCompetition(gameId));
    }

    @PutMapping("/score")
    @Operation(summary = "修正整体提案赛小组得分")
    public Result<Void> adjustScore(@RequestBody @Validated ProposalScoreAdjustReq req) {
        proposalService.adjustGlobalScore(req);
        return Result.success();
    }

    @GetMapping("/second/need")
    @Operation(summary = "查询第二轮提案赛需要打分的小组")
    public Result<List<Long>> listNeedScore(@RequestParam Long gameId) {
        return Result.success(proposalService.listNeedScore(gameId));
    }

}
