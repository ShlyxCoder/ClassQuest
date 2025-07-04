package cn.org.shelly.edu.controller;

import cn.org.shelly.edu.common.Result;
import cn.org.shelly.edu.model.pojo.BoardConfig;
import cn.org.shelly.edu.model.pojo.Game;
import cn.org.shelly.edu.model.pojo.Team;
import cn.org.shelly.edu.model.pojo.TeamMember;
import cn.org.shelly.edu.model.req.BoardInitReq;
import cn.org.shelly.edu.model.req.GameInitReq;
import cn.org.shelly.edu.model.resp.RankResp;
import cn.org.shelly.edu.model.resp.TeamUploadResp;
import cn.org.shelly.edu.service.BoardConfigService;
import cn.org.shelly.edu.service.GameService;
import cn.org.shelly.edu.service.TeamMemberService;
import cn.org.shelly.edu.service.TeamService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/game")
@RequiredArgsConstructor
@Tag(name= "游戏管理")
public class GameController {
    private final GameService gameService;
    private final BoardConfigService boardConfigService;
    private final TeamService teamService;
    private final TeamMemberService teamMemberService;
    @PostMapping("/upload")
    @Operation(summary = "上传游戏分组导入")
    public Result<TeamUploadResp> init(GameInitReq req){
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

    private String listToStr(List<Integer> list) {
        return list == null ? null : list.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    @GetMapping("/{id}")
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
    @GetMapping("/rank/{id}")
    @Operation(summary = "获取游戏排名")
    public Result<RankResp> getGameRank(@PathVariable("id") @Schema(description = "游戏ID") Long id) {
        Game game = gameService.getById(id);
        if (game == null) {
            return Result.fail("游戏不存在");
        }
        var rankResp = new RankResp();
        List<RankResp.TeamRankDTO> teamRanks = teamService.lambdaQuery()
                .select(Team::getId, Team::getLeaderName, Team::getTotalScore)
                .eq(Team::getGameId, id)
                .orderByDesc(Team::getTotalScore)
                .orderByAsc(Team::getLeaderName)
                .list()
                .stream()
                .map(team -> {
                    RankResp.TeamRankDTO dto = new RankResp.TeamRankDTO();
                    dto.setTeamId(team.getId());
                    dto.setLeaderName(team.getLeaderName());
                    dto.setTotalScore(team.getTotalScore());
                    return dto;
                }).toList();
        // 获取当前游戏下所有团队ID
        List<Long> teamIds = teamRanks.stream()
                .map(RankResp.TeamRankDTO::getTeamId)
                .toList();

        List<RankResp.StudentRankDTO> studentRanks = teamMemberService.lambdaQuery()
                .in(TeamMember::getTeamId, teamIds)
                .select(TeamMember::getStudentId, TeamMember::getStudentName, TeamMember::getIndividualScore, TeamMember::getTeamId)
                .orderByDesc(TeamMember::getIndividualScore)
                .orderByAsc(TeamMember::getStudentName)
                .list()
                .stream()
                .map(member -> {
                    RankResp.StudentRankDTO dto = new RankResp.StudentRankDTO();
                    dto.setStudentId(member.getStudentId());
                    dto.setStudentName(member.getStudentName());
                    dto.setIndividualScore(member.getIndividualScore());
                    dto.setTeamId(member.getTeamId());
                    return dto;
                }).toList();
        rankResp.setTeamRanks(teamRanks);
        rankResp.setStudentRanks(studentRanks);
        return Result.success(rankResp);
    }
    @PostMapping("/upload/chess")
    @Operation(summary = "上传棋盘赛学习通成绩")
    public Result<Boolean> uploadChessResult(@RequestParam MultipartFile file,
                                             @RequestParam Long gameId) {
        return Result.success(gameService.upload(file, gameId));
    }

    @PostMapping("/tile/occupy")
    @Operation(summary = "上传领地选择")
    public Result<Void> uploadTileOccupy() {
        return Result.fail("服务未实现");
    }
    @PostMapping("/tile/change")
    @Operation(summary = "领地变更事件")
    public Result<Void> uploadTileChange() {
        return Result.fail("服务未实现");
    }
    @PutMapping("/score")
    @Operation(summary = "积分变更")
    public Result<Void> updateScore() {
        return Result.fail("服务未实现");
    }


}
