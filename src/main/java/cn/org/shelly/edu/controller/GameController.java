package cn.org.shelly.edu.controller;

import cn.org.shelly.edu.common.Result;
import cn.org.shelly.edu.exception.CustomException;
import cn.org.shelly.edu.model.pojo.BoardConfig;
import cn.org.shelly.edu.model.pojo.Game;
import cn.org.shelly.edu.model.pojo.Team;
import cn.org.shelly.edu.model.pojo.TeamMember;
import cn.org.shelly.edu.model.req.BoardInitReq;
import cn.org.shelly.edu.model.req.GameInitReq;
import cn.org.shelly.edu.model.req.ScoreUpdateReq;
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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
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
    @GetMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
    @GetMapping("/rank/team/{id}")
    @Operation(summary = "棋盘赛获取小组排名")
    public Result<List<RankResp.TeamRankDTO>> getTeamRank(@PathVariable("id") @Schema(description = "游戏ID") Long id) {
        Game game = gameService.getById(id);
        if (game == null) {
            return Result.fail("游戏不存在");
        }
        List<RankResp.TeamRankDTO> teamRanks = teamService.lambdaQuery()
                .select(Team::getId, Team::getLeaderName, Team::getBoardScoreAdjusted,
                        Team::getTotalMembers, Team::getSno, Team::getMemberScoreSum)
                .eq(Team::getGameId, id)
                .list()
                .stream()
                .map(team -> {
                    RankResp.TeamRankDTO dto = new RankResp.TeamRankDTO();
                    dto.setTeamId(team.getId());
                    dto.setLeaderName(team.getLeaderName());
                    int totalScore = team.getMemberScoreSum() + team.getBoardScoreAdjusted();
                    dto.setTotalScore(totalScore);
                    dto.setTotalMembers(team.getTotalMembers());
                    dto.setLeaderSno(team.getSno());
                    return dto;
                })
                .sorted(Comparator.comparingInt(RankResp.TeamRankDTO::getTotalScore).reversed()
                        .thenComparing(RankResp.TeamRankDTO::getLeaderName))
                .toList();
        return Result.success(teamRanks);
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


    @GetMapping("/upload/chess")
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
    @PostMapping("/score/update")
    @Operation(summary = "老师操作积分变更")
    public Result<Void> updateScore(@RequestBody ScoreUpdateReq req) {
        Integer type = req.getType();
        Integer stage = req.getStage();
        Long id = req.getId();
        Long gameId = req.getGameId();
        Integer num = req.getNum();
        String comment = req.getComment();
        if (type == 1) { // 小组
            Team team = teamService.lambdaQuery()
                    .eq(Team::getId, id)
                    .eq(Team::getGameId, gameId)
                    .one();
            if (team == null) {
                throw new CustomException("小组不存在");
            }
            if (stage == 1) {
                team.setBoardScoreAdjusted(team.getBoardScoreAdjusted() + num);
            } else if (stage == 2) {
                team.setProposalScoreAdjusted(team.getProposalScoreAdjusted() + num);
            }
            teamService.lambdaUpdate()
                    .eq(Team::getId, team.getId())
                    .eq(Team::getGameId, gameId)
                    .update(team);
            // TODO: 小组积分变更通知（建议日志记录 comment）
        } else { // 个人
            TeamMember member = teamMemberService.getById(id);
            if (member == null) {
                throw new CustomException("成员不存在");
            }
            member.setIndividualScore(member.getIndividualScore() + num);
            teamMemberService.updateById(member);

            Long teamId = member.getTeamId();
            Team team = teamService.lambdaQuery()
                    .eq(Team::getId, teamId)
                    .eq(Team::getGameId, gameId)
                    .one();
            if (team == null) {
                throw new CustomException("成员所属小组不存在");
            }
            Game game = gameService.getById(team.getGameId());
            if (game == null) {
                throw new CustomException("游戏不存在");
            }
            int maxCount = game.getTeamMemberCount();
            List<TeamMember> members = teamMemberService.lambdaQuery()
                    .eq(TeamMember::getTeamId, teamId)
                    .orderByDesc(TeamMember::getIndividualScore)
                    .last("LIMIT " + maxCount)
                    .list();
            int totalScore = members.stream()
                    .mapToInt(TeamMember::getIndividualScore)
                    .sum();
            team.setMemberScoreSum(totalScore);
            teamService.lambdaUpdate()
                    .eq(Team::getId, team.getId())
                    .eq(Team::getGameId, team.getGameId())
                    .update(team);
            // TODO: 个人积分变更通知（建议日志记录 comment）
        }
        return Result.success();
    }



}
