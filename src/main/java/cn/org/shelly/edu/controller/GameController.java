package cn.org.shelly.edu.controller;

import cn.org.shelly.edu.annotation.ScoreLogComment;
import cn.org.shelly.edu.common.Result;
import cn.org.shelly.edu.exception.CustomException;
import cn.org.shelly.edu.model.pojo.*;
import cn.org.shelly.edu.model.req.*;
import cn.org.shelly.edu.model.resp.*;
import cn.org.shelly.edu.service.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/game")
@RequiredArgsConstructor
@Tag(name= "棋盘赛管理")
public class GameController {
    private final GameService gameService;
    private final BoardConfigService boardConfigService;
    private final TeamMemberService teamMemberService;
    @PostMapping(value = "/upload")
    @Operation(summary = "上传游戏分组导入")
    public Result<TeamUploadResp> init(
            @RequestPart("file") MultipartFile file,
            @RequestParam("teamNum") Integer teamNum,
            @RequestParam("studentNum") Integer studentNum,
            @RequestParam("teamMemberCount") Integer teamMemberCount,
            @RequestParam("cid") Long cid
    ) {
        GameInitReq req = new GameInitReq();
        req.setFile(file);
        req.setTeamNum(teamNum);
        req.setStudentNum(studentNum);
        req.setTeamMemberCount(teamMemberCount);
        req.setCid(cid);
        return Result.success(gameService.init(req));
    }

    @PostMapping("/board/init")
    @Operation(summary = "初始化棋盘配置")
    public Result<Void> initBoardConfig(@RequestBody @Valid BoardInitReq req) {
        if (req.getGameId() == null) {
            return Result.fail("游戏ID不能为空");
        }
        Game game = gameService.getById(req.getGameId());
        if (game == null) {
            return Result.fail("游戏不存在");
        }
        if (game.getStage() != 0 ) {
            return Result.fail("当前阶段不能开始初始化");
        }
        BoardConfig config = new BoardConfig();
        config.setGameId(req.getGameId());
        config.setTotalTiles(req.getTotalTiles());
        config.setBlackSwampTiles(listToStr(req.getBlackSwampTiles()));
        config.setBlindBoxTiles(listToStr(req.getBlindBoxTiles()));
        config.setFortressTiles(listToStr(req.getFortressTiles()));
        config.setGoldCenterTiles(listToStr(req.getGoldCenterTiles()));
        config.setOpportunityTiles(listToStr(req.getOpportunityTiles()));
        boardConfigService.save(config);
        game.setStatus(1);
        game.setStage(1);
        game.setChessRound(1);
        game.setChessPhase(0);
        game.setLastSavedAt(new java.util.Date());
        gameService.updateById(game);
        return Result.success();
    }

    private String listToStr(Object list) {
        try {
            return new ObjectMapper().writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new CustomException("序列化失败");
        }
    }


    @GetMapping("/status/{id}")
    @Operation(summary = "获取游戏状态")
    public Result<Game> getGameStatus(@PathVariable("id") Long id) {
        return Result.success(gameService.getById(id));
    }
    @GetMapping("/list/{cid}")
    @Operation(summary = "获取游戏列表")
    public Result<List<Game>> listGame(@PathVariable("cid") Long cid) {
        var list = gameService.list(new QueryWrapper<Game>().eq("cid", cid).orderByDesc("gmt_update"));
        return Result.success(list);
    }
    @GetMapping("/rank/team/{id}")
    @Operation(summary = "棋盘赛获取小组领地排名")
    public Result<List<TeamRankResp>> getTeamRank(@PathVariable("id") @Schema(description = "游戏ID") Long id) {
        Game game = gameService.getById(id);
        if (game == null) {
            return Result.fail("游戏不存在");
        }
        return Result.success(gameService.getTeamRank(game));
    }
    @GetMapping("/rank/student/{id}")
    @Operation(summary = "棋盘赛获取学生排名")
    public Result<List<RankResp.StudentRankDTO>> getStudentRank(@PathVariable("id") @Schema(description = "游戏ID") Long id) {
        Game game = gameService.getById(id);
        if (game == null) {
            return Result.fail("游戏不存在");
        }
        List<RankResp.StudentRankDTO> studentRanks = teamMemberService.lambdaQuery()
                .eq(TeamMember::getGameId, id)
                .select(TeamMember::getStudentId, TeamMember::getStudentName, TeamMember::getSno,
                        TeamMember::getIndividualScore, TeamMember::getTeamId)
                .orderByDesc(TeamMember::getIndividualScore)
                .orderByAsc(TeamMember::getStudentName)
                .list()
                .stream()
                .map(member -> {
                    RankResp.StudentRankDTO dto = new RankResp.StudentRankDTO();
                    dto.setStudentId(member.getStudentId());
                    dto.setStudentName(member.getStudentName());
                    dto.setMemberSno(member.getSno());
                    dto.setIndividualScore(member.getIndividualScore());
                    dto.setTeamId(member.getTeamId());
                    return dto;
                })
                .toList();
        return Result.success(studentRanks);
    }


    @PostMapping("/upload/chess")
    @ScoreLogComment
    @Operation(summary = "上传棋盘赛学习通成绩")
    public Result<List<TeamScoreRankResp>> uploadChessResult(@RequestPart("file") MultipartFile file,
                                                             @RequestParam Long gameId) {
        return Result.success(gameService.upload(file, gameId));
    }
    @GetMapping("/xxt/rank/{id}")
    @Operation(summary = "棋盘赛获取学习通排名")
    public Result<List<TeamScoreRankResp>> getXXTStudentRank(@PathVariable("id") @Schema(description = "游戏ID") Long id) {
        return Result.success(gameService.getStudentRank(id));
    }
    @PostMapping("/upload/assign")
    @Operation(summary = "上传排名分配")
    public Result<Void> uploadAssign(@RequestBody AssignReq req) {
        gameService.uploadAssign(req);
        return Result.success();
    }
    @GetMapping("/occupyStatus/{gameId}")
    @Operation(summary = "查看棋盘占有")
    public Result<BoardResp> getTileOccupy(@PathVariable @Schema(description = "游戏ID") Long gameId){
        return Result.success(gameService.showOccupyStatus(gameId));
    }


    @PostMapping("/tile/occupy")
    @Operation(summary = "上传领地选择")
    public Result<Boolean> uploadTileOccupy(
          @RequestBody TileOccupyReq req
    ) {
        try {
            return Result.success(gameService.occupy(req,2));
        } catch (JsonProcessingException e) {
            throw new CustomException(e);
        }
    }
    @GetMapping("/unselected/{gameId}")
    @Operation(summary = "查看当前游戏轮次未完成格子选择的小组",description = "若为空时，自动触发下一状态")
    public Result<List<UnselectedTeamResp>> getUnselectedTeams(@PathVariable("gameId") Long gameId) {
        List<UnselectedTeamResp> list = gameService.getUnselectedTeamsByGame(gameId);
        return Result.success(list);
    }

    @PostMapping("/special/blind-box/settle")
    @Operation(summary = "结算盲盒秘境触发结果")
    public Result<Void> settleBlindBoxEvent(@RequestBody BlindBoxSettleReq req) {
        gameService.settleBlindBoxEvent(req);
        return Result.success();
    }
    @PostMapping("/special/fortress/settle")
    @Operation(summary = "结算决斗要塞结果(已弃用)")
    public Result<Void> settleFortressBattle(@RequestBody FortressBattleReq req) {
        try {
            gameService.settleFortressBattle(req);
        } catch (JsonProcessingException e) {
            throw new CustomException(e);
        }
        return Result.success();
    }
    @PostMapping("/special/opportunity/settle")
    @Operation(summary = "结算机会宝地任务")
    public Result<Void> settleOpportunityTask(@RequestBody OpportunitySettleReq req) {
        gameService.settleOpportunityTask(req);
        return Result.success();
    }


    @GetMapping("/special/list")
    @Operation(summary = "获取当前轮次特殊效果列表")
    public Result<List<TeamSpecialEffectResp>> getSpecialEffectList(@RequestParam Long gameId) {
        List<TeamSpecialEffectResp> list = gameService.getSpecialEffectList(gameId);
        return Result.success(list);
    }

    @GetMapping("/boardConfig")
    @Operation(summary = "获取棋盘配置(弃用)")
    public Result<BoardConfig> getBoardConfig(@RequestParam Long gameId) {
        BoardConfig boardConfig = boardConfigService.lambdaQuery()
                .eq(BoardConfig::getGameId, gameId)
                .one();
        return Result.success(boardConfig);
    }
    @PostMapping("/score/update")
    @Operation(summary = "棋盘赛老师操作积分变更")
    public Result<Void> updateScore(@RequestBody ScoreUpdateReq req) {
        gameService.updateScore(req);
        return Result.success();
    }
    @PostMapping("/comment")
    @Operation(summary = "全局添加评论接口")
    public Result<Void> addComment(@RequestBody CommentReq req) {
        gameService.addComment(req);
        return Result.success();
    }



}
