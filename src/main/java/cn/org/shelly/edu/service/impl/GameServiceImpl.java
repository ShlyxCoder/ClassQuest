package cn.org.shelly.edu.service.impl;

import cn.org.shelly.edu.enums.CodeEnum;
import cn.org.shelly.edu.exception.CustomException;
import cn.org.shelly.edu.exception.ExcelReadStopException;
import cn.org.shelly.edu.listener.XxtStudentScoreListener;
import cn.org.shelly.edu.mapper.GameMapper;
import cn.org.shelly.edu.mapper.TeamMemberMapper;
import cn.org.shelly.edu.model.dto.*;
import cn.org.shelly.edu.model.pojo.*;
import cn.org.shelly.edu.model.req.*;
import cn.org.shelly.edu.model.resp.*;
import cn.org.shelly.edu.service.*;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameServiceImpl extends ServiceImpl<GameMapper, Game>
    implements GameService {
    private final ClassStudentService classStudentService;
    private final TeamService teamService;
    private final TeamMemberService teamMemberService;
    private final ClassService classService;
    private final TeamTileActionService teamTileActionService;
    private final BoardConfigService boardConfigService;
    private final TeamMemberMapper teamMemberMapper;
    private final StudentScoreLogService studentScoreLogService;
    private final TeamScoreLogService teamScoreLogService;
    @Resource
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;


    @Override
    public TeamUploadResp init(GameInitReq req) {
        Game game = createGame(req);
        List<TeamInfoDTO> groupList = parseExcel(req.getFile());
        log.info(groupList.toString());
        //注意：以学号做key，确保唯一性
        Map<String, ClassStudent> studentMap = getClassStudentMap(req.getCid());
        List<ClassStudent> newStudents = collectMissingStudents(groupList, studentMap, req.getCid());
        if (!newStudents.isEmpty()) {
            classStudentService.saveBatch(newStudents);
            for (ClassStudent s : newStudents) {
                studentMap.put(s.getSno(), s);
            }
        }
        TeamUploadDTO dto = saveTeamsAndMembers(groupList, game, studentMap);
        Long count = classStudentService
                .lambdaQuery()
                .eq(ClassStudent::getCid, req.getCid())
                .count();
        classService.lambdaUpdate()
                .eq(Classes::getId, req.getCid())
                .set(Classes::getCurrentStudents, count)
                .update();
        return buildUploadResp(req, newStudents.size(), dto, game.getId());
    }

    @Override
    public List<TeamScoreRankResp> upload(MultipartFile file, Long gameId) {
        // 1. 验证游戏状态
        Game game = getById(gameId);
        validateGameState(game);
        // 2. 读取并验证上传文件
        List<XxtStudentScoreExcelDTO> scores = validateAndParseFile(file);
        // 3. 根据上传成绩构建：teamId -> 成绩列表 映射
        Map<Long, List<XxtStudentScoreExcelDTO>> groupMap = buildTeamGroupMap(scores, gameId, game.getCid());
        // 4. 获取该游戏所有小组映射：teamId -> Team
        Map<Long, Team> teamMap = getTeamMapByGame(gameId);
        // 5. 计算本轮小组得分，更新累计得分，并生成排行榜响应
        List<TeamScoreRankResp> rankList = calculateAndUpdateTeamScoresAndGetRankResp(groupMap, game, teamMap);
        // 6. 更新游戏阶段等信息
        game.setChessPhase(1);
        game.setStage(1);
        game.setLastSavedAt(new Date());
        updateById(game);
        // 8. 构造得分日志列表
        List<TeamScoreLog> logs = rankList.stream().map(item -> {
            TeamScoreLog log = new TeamScoreLog();
            log.setGameId(gameId);
            log.setTeamId(item.getTeamId());
            log.setScore(item.getThisRoundScore());
            log.setReason(3);
            log.setPhase(1);
            log.setRound(game.getChessRound());
            log.setComment("该积分是棋盘赛从学习通导入成绩获得");
            log.setSubmitTime(item.getSubmitTime());
            return log;
        }).toList();
        // 9. 批量插入日志
        teamScoreLogService.saveBatch(logs);
        // 获取所有学生信息，或许可以放入异步？
        List<TeamMember> members = teamMemberService.lambdaQuery()
                .eq(TeamMember::getGameId, gameId)
                .list();
        Map<String, TeamMember> memberMap = members.stream()
                .collect(Collectors.toMap(TeamMember::getSno, m -> m, (a, b) -> a));
        threadPoolTaskExecutor.execute(() -> {
            List<StudentScoreLog> studentLogs = scores.stream()
                    .map(dto -> {
                        TeamMember member = memberMap.get(dto.getSno());
                        if (member == null) {
                            return null;
                        }
                        StudentScoreLog log = new StudentScoreLog();
                        log.setStudentId(member.getStudentId());
                        log.setTeamId(member.getTeamId());
                        log.setGameId(gameId);
                        log.setScore(parseScore(dto.getScore()));
                        log.setReason(3);
                        log.setPhase(1);
                        log.setRound(game.getChessRound());
                        log.setComment("第" + game.getChessRound() + "轮棋盘赛学习通导入中获得成绩，满分40分，实际得分为" + dto.getScore() + "分");
                        return log;
                    })
                    .filter(Objects::nonNull)
                    .toList();
            studentScoreLogService.saveBatch(studentLogs);
        });
        return rankList;
    }
    private int parseScore(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            return 0;
        }
    }


    @Override
    public void uploadAssign(AssignReq req) {
        Long gameId = req.getGameId();
        Game game = getById(gameId);
        Map<Long, Integer> teamAssignCount = req.getTeamAssignCount();
        if (game == null) {
            throw new CustomException("游戏不存在");
        }
        if (game.getStage() != 1 || game.getChessPhase() != 1) {
            throw new CustomException("当前阶段不能上传领地");
        }
        List<TeamTileAction> teamTileActions = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : teamAssignCount.entrySet()) {
            TeamTileAction teamTileAction = new TeamTileAction();
            teamTileAction.setGameId(game.getId());
            teamTileAction.setTeamId(entry.getKey());
            teamTileAction.setRound(game.getChessRound());
            teamTileAction.setPhase(1);
            teamTileAction.setOriginalTileCount(entry.getValue());
            teamTileAction.setSelected(0);
            teamTileActions.add(teamTileAction);
        }
        teamTileActionService.saveBatch(teamTileActions);
        game.setChessPhase(2);
        game.setLastSavedAt(new java.util.Date());
        updateById(game);
    }

    @Override
    public List<UnselectedTeamResp> getUnselectedTeamsByGame(Long gameId) {
        Game game = getById(gameId);
        if (game == null) {
            throw new CustomException("游戏不存在");
        }
        if (game.getChessPhase() != 2) {
            throw new CustomException("当前不是走棋阶段");
        }
        Map<Long, TeamTileAction> unselectedMap = teamTileActionService.lambdaQuery()
                .eq(TeamTileAction::getGameId, gameId)
                .eq(TeamTileAction::getRound, game.getChessRound())
                .eq(TeamTileAction::getSelected, 0)
                .list()
                .stream()
                .collect(Collectors.toMap(TeamTileAction::getTeamId, action -> action));
        log.info("未选择的小组：{}", unselectedMap.keySet());
        if (unselectedMap.isEmpty()) {
            game.setChessPhase(3);
            game.setLastSavedAt(new java.util.Date());
            updateById(game);
            return Collections.emptyList();
        }
        List<Long> sortedTeamIds = teamScoreLogService.lambdaQuery()
                .select(TeamScoreLog::getTeamId)
                .eq(TeamScoreLog::getGameId, gameId)
                .eq(TeamScoreLog::getPhase, 1)
                .eq(TeamScoreLog::getReason, 3)
                .eq(TeamScoreLog::getRound, game.getChessRound())
                .in(TeamScoreLog::getTeamId, unselectedMap.keySet())
                .orderByDesc(TeamScoreLog::getScore)
                .orderByAsc(TeamScoreLog::getSubmitTime)
                .list()
                .stream()
                .map(TeamScoreLog::getTeamId)
                .distinct()
                .toList();
        log.info("已排序的小组：{}", sortedTeamIds);
        Map<Long, Team> teamMap = teamService.lambdaQuery()
                .eq(Team::getGameId, gameId)
                .in(Team::getId, unselectedMap.keySet())
                .list()
                .stream()
                .collect(Collectors.toMap(Team::getId, t -> t));
        List<UnselectedTeamResp> result = new ArrayList<>();
        for (Long teamId : sortedTeamIds) {
            Team team = teamMap.get(teamId);
            TeamTileAction action = unselectedMap.get(teamId);
            if (team != null && action != null) {
                UnselectedTeamResp resp = new UnselectedTeamResp();
                resp.setTeamId(teamId);
                resp.setLeaderName(team.getLeaderName());
                resp.setSno(team.getSno());
                resp.setAssignCount(action.getOriginalTileCount());
                resp.setLeaderId(team.getLeaderId());
                result.add(resp);
            }
        }
        return result;
    }
    @Override
    public Boolean occupy(TileOccupyReq req, Integer status) throws JsonProcessingException {
        Game game = getById(req.getGameId());
        if (game == null) {
            throw new CustomException("游戏不存在");
        }
        if (game.getStage() != 1) {
            throw new CustomException("当前不是棋盘赛阶段");
        }
        if (!Objects.equals(game.getChessPhase(), status)) {
            throw new CustomException("当前不是合法阶段");
        }
        if(Boolean.TRUE.equals(req.getTriggerChanceLand()) && game.getChessRound() < 3){
            log.info(game.getChessPhase().toString());

            throw new CustomException("当前不是触发机会宝地阶段");
        }
        BoardConfig config = boardConfigService.lambdaQuery()
                .eq(BoardConfig::getGameId, req.getGameId())
                .one();
        if (config == null) {
            throw new CustomException("游戏配置不存在");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        List<BoardInitReq.BlindBoxTile> blindBoxTilesList = objectMapper
                .readValue(config.getBlindBoxTiles(), new TypeReference<>() {});
        List<BoardInitReq.FortressTile> fortressTilesList = objectMapper
                .readValue(config.getFortressTiles(), new TypeReference<>() {});
        Set<Integer> goldCenterSet = objectMapper
                .readValue(config.getGoldCenterTiles(), new TypeReference<>() {});
        Set<Integer> opportunitySet = objectMapper
                .readValue(config.getOpportunityTiles(), new TypeReference<>() {});

        List<Integer> selectedTiles = req.getTileIds();
        if (selectedTiles == null || selectedTiles.isEmpty()) {
            throw new CustomException("请选择至少一个格子");
        }
        if (Boolean.TRUE.equals(req.getTriggerChanceLand())
                && (req.getChanceLandTileId() == null || !opportunitySet.contains(req.getChanceLandTileId()))) {
            throw new CustomException("触发的机会宝地格子不存在");
        }

        // 分类：将选择的格子按类型分类
        List<BoardInitReq.BlindBoxTile> blindBoxSelected = new ArrayList<>();
        List<BoardInitReq.FortressTile> fortressSelected = new ArrayList<>();
        List<Integer> goldCenterSelected = new ArrayList<>();

        for (Integer tileId : selectedTiles) {
            blindBoxTilesList.stream()
                    .filter(b -> b.getTileId().equals(tileId))
                    .findFirst()
                    .ifPresent(blindBoxSelected::add);
            fortressTilesList.stream()
                    .filter(f -> f.getTileId().equals(tileId))
                    .findFirst()
                    .ifPresent(fortressSelected::add);
            if (goldCenterSet.contains(tileId)) {
                goldCenterSelected.add(tileId);
            }
        }
        // 剔除触发的盲盒
        if (Boolean.TRUE.equals(req.getTriggerBlindBox()) && req.getBlindBoxTileIds() != null) {
            Set<Integer> triggered = new HashSet<>(req.getBlindBoxTileIds());
            blindBoxSelected = blindBoxSelected.stream()
                    .filter(b -> !(triggered.contains(b.getTileId()) && b.getEventType() == 0))
                    .toList();
        }
        // 剔除触发的黄金中心
        if (Boolean.TRUE.equals(req.getTriggerGoldCenter()) && req.getGoldCenterTileId() != null) {
            goldCenterSelected = goldCenterSelected.stream()
                    .filter(id -> !id.equals(req.getGoldCenterTileId()))
                    .toList();
        }
        // 获取 action 记录
        TeamTileAction action = teamTileActionService.lambdaQuery()
                .eq(TeamTileAction::getGameId, req.getGameId())
                .eq(TeamTileAction::getTeamId, req.getTeamId())
                .eq(TeamTileAction::getRound, game.getChessRound())
                .one();
        if (action == null) {
            throw new CustomException("未找到该队伍的行动记录");
        }
        action.setPhase(2);
        action.setAllTiles(mergeTiles(action.getAllTiles(), selectedTiles));
        action.setBlindBoxTiles(mergeJsonList(action.getBlindBoxTiles(), blindBoxSelected, BoardInitReq.BlindBoxTile::getTileId, BoardInitReq.BlindBoxTile.class));
        action.setFortressTiles(mergeJsonList(action.getFortressTiles(), fortressSelected, BoardInitReq.FortressTile::getTileId, BoardInitReq.FortressTile.class));
        action.setGoldCenterTiles(mergeTiles(action.getGoldCenterTiles(), goldCenterSelected));
        action.setOpportunityTiles(null);
        action.setSettledTileCount(mergeTilesToCount(action.getAllTiles()));
        action.setSelected(1);
        boolean saved = teamTileActionService.updateById(action);
        if (!saved) {
            throw new CustomException("保存格子选择记录失败");
        }
        return true;
    }



    @Override
    public List<TeamRankResp> getTeamRank(Game game) {
        List<TeamTileAction> actions = teamTileActionService.lambdaQuery()
                .eq(TeamTileAction::getGameId, game.getId())
                .eq(TeamTileAction::getSelected, 1)
                .list();
        Map<Long, Integer> teamToTotalTile = new HashMap<>();
        for (TeamTileAction action : actions) {
            int settledCount = Optional.ofNullable(action.getSettledTileCount()).orElse(0);
            teamToTotalTile.merge(action.getTeamId(), settledCount, Integer::sum);
        }

        List<Team> teams = teamService.lambdaQuery()
                .eq(Team::getGameId, game.getId())
                .list();

        List<TeamRankResp> rankList = new ArrayList<>();
        for (Team team : teams) {
            TeamRankResp resp = new TeamRankResp();
            resp.setTeamId(team.getId());
            resp.setLeaderName(team.getLeaderName());
            resp.setTotalScore(Optional.ofNullable(team.getMemberScoreSum()).orElse(0));
            resp.setTotalTile(teamToTotalTile.getOrDefault(team.getId(), 0));
            resp.setLeaderSno(team.getSno());
            resp.setStatus(team.getAlive());
            rankList.add(resp);
        }
        // 按总领地数降序排序
        rankList.sort(Comparator.comparingInt(TeamRankResp::getTotalTile).reversed()
                .thenComparing(Comparator.comparingInt(TeamRankResp::getTotalScore).reversed()));
        return rankList;
    }

    @Override
    public BoardResp showOccupyStatus(Long gameId) {
        // 1. 查询当前游戏的所有已提交格子数据
        List<TeamTileAction> actions = teamTileActionService.lambdaQuery()
                .eq(TeamTileAction::getGameId, gameId)
                .list();

        // 2. 获取游戏棋盘配置
        BoardConfig config = boardConfigService.lambdaQuery()
                .eq(BoardConfig::getGameId, gameId)
                .one();
        if (config == null) {
            throw new CustomException("游戏配置不存在");
        }

        ObjectMapper mapper = new ObjectMapper();
        List<Integer> blackSwampTiles;
        List<BoardInitReq.BlindBoxTile> blindBoxTiles;
        List<BoardInitReq.FortressTile> fortressTiles;
        List<Integer> goldCenterTiles;
        List<Integer> opportunityTiles;

        try {
            blackSwampTiles = mapper.readValue(config.getBlackSwampTiles(), new TypeReference<>() {});
            blindBoxTiles = mapper.readValue(config.getBlindBoxTiles(), new TypeReference<>() {});
            fortressTiles = mapper.readValue(config.getFortressTiles(), new TypeReference<>() {});
            goldCenterTiles = mapper.readValue(config.getGoldCenterTiles(), new TypeReference<>() {});
            opportunityTiles = mapper.readValue(config.getOpportunityTiles(), new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new CustomException("解析棋盘配置失败");
        }

        // 3. 小组 -> 占领格子映射
        Map<Long, List<Integer>> teamTilesMap = new HashMap<>();
        for (TeamTileAction action : actions) {
            List<Integer> tiles = parseTiles(action.getAllTiles());
            teamTilesMap.computeIfAbsent(action.getTeamId(), k -> new ArrayList<>()).addAll(tiles);
        }

        // 4. 构造 BoardResp
        BoardResp resp = new BoardResp();
        resp.setTotalTiles(config.getTotalTiles());
        resp.setBlackSwampTiles(blackSwampTiles);

        // 盲盒格子排序输出：eventType: 0（2个），1（1个），2（1个）
        List<Integer> sortedBlindBox = blindBoxTiles.stream()
                .sorted(Comparator.comparingInt(BoardInitReq.BlindBoxTile::getEventType))
                .map(BoardInitReq.BlindBoxTile::getTileId)
                .toList();
        resp.setBlindBoxTiles(sortedBlindBox);

        // 决斗要塞格子排序输出：gameType: 0（2个），1（2个）
        List<Integer> sortedFortress = fortressTiles.stream()
                .sorted(Comparator.comparingInt(BoardInitReq.FortressTile::getGameType))
                .map(BoardInitReq.FortressTile::getTileId)
                .toList();
        resp.setFortressTiles(sortedFortress);

        resp.setGoldCenterTiles(goldCenterTiles);
        resp.setOpportunityTiles(opportunityTiles);

        List<TeamTileResp> teamResp = teamTilesMap.entrySet().stream()
                .map(entry -> {
                    TeamTileResp team = new TeamTileResp();
                    team.setTeamId(entry.getKey());
                    team.setOccupiedTiles(entry.getValue());
                    return team;
                }).toList();
        resp.setTeams(teamResp);

        return resp;
    }

    @Override
    public List<TeamSpecialEffectResp> getSpecialEffectList(Long gameId) {
        Game game = getById(gameId);
        if(game == null){
            throw  new CustomException("游戏不存在");
        }
        if(game.getStage() != 1 || game.getStatus() != 1){
            throw new CustomException("该状态下不可进行该操作");
        }
        if(game.getChessPhase() != 3){
            throw new CustomException("当前未处于轮次结算阶段");
        }
        List<TeamTileAction> actions = teamTileActionService.lambdaQuery()
                .eq(TeamTileAction::getGameId, gameId)
                .eq(TeamTileAction::getRound, game.getChessRound())
                .eq(TeamTileAction::getPhase, 2)
                .eq(TeamTileAction::getSelected, 1)
                .list();
        ObjectMapper mapper = new ObjectMapper();
        List<TeamSpecialEffectResp> result = new ArrayList<>();

        for (TeamTileAction action : actions) {
            List<SpecialTileResp> specialList = new ArrayList<>();
            try {
                List<BoardInitReq.BlindBoxTile> blindBoxTiles =
                        mapper.readValue(action.getBlindBoxTiles(), new TypeReference<>() {});
                for (BoardInitReq.BlindBoxTile tile : blindBoxTiles) {
                    SpecialTileResp resp = new SpecialTileResp();
                    resp.setTileId(tile.getTileId());
                    resp.setTileType(1);
                    resp.setEventType(tile.getEventType());
                    resp.setEventName(tile.getEventType() == 1 ? "图片论述" : "五词对抗");
                    specialList.add(resp);
                }
            } catch (Exception ignored) {
                log.error("获取特殊格子列表失败", ignored);
            }

            try {
                List<BoardInitReq.FortressTile> fortressTiles =
                        mapper.readValue(action.getFortressTiles(), new TypeReference<>() {});
                for (BoardInitReq.FortressTile tile : fortressTiles) {
                    SpecialTileResp resp = new SpecialTileResp();
                    resp.setTileId(tile.getTileId());
                    resp.setTileType(2);
                    resp.setEventType(tile.getGameType());
                    resp.setEventName(tile.getGameType() == 0 ? "双音节成语" : "成语抢答");
                    specialList.add(resp);
                }
            } catch (Exception ignored) {
                log.error("获取游戏{}的黄金中心格子信息失败", gameId);
            }
            try {
                List<Integer> goldCenter =
                        mapper.readValue(action.getGoldCenterTiles(), new TypeReference<>() {});
                for (Integer tileId : goldCenter) {
                    SpecialTileResp resp = new SpecialTileResp();
                    resp.setTileId(tileId);
                    resp.setTileType(3);
                    resp.setEventType(null);
                    resp.setEventName("黄金中心");
                    specialList.add(resp);
                }
            } catch (Exception ignored) {
                log.error("获取黄金中心格子信息失败", ignored);
            }
            try {
                List<Integer> opportunity =
                        mapper.readValue(action.getOpportunityTiles(), new TypeReference<>() {});
                for (Integer tileId : opportunity) {
                    SpecialTileResp resp = new SpecialTileResp();
                    resp.setTileId(tileId);
                    resp.setTileType(4);
                    resp.setEventType(null);
                    resp.setEventName("机会宝地");
                    specialList.add(resp);
                }
            } catch (Exception ignored) {
                log.error("获取特殊格子列表失败", ignored);
            }

            if (!specialList.isEmpty()) {
                TeamSpecialEffectResp teamResp = new TeamSpecialEffectResp();
                teamResp.setTeamId(action.getTeamId());
                teamResp.setUnTriggeredTiles(specialList);
                result.add(teamResp);
            }
        }
        if (result.isEmpty()) {
            log.info("当前轮次所有队伍已完成所有特殊格子事件。");
            game.setChessPhase(0);
            game.setChessRound(game.getChessRound() + 1);
            if(game.getChessRound() > 4){
                game.setStage(2);
                game.setProposalStage(0);
                game.setProposalRound(0);
            }
            game.setLastSavedAt(new java.util.Date());
            updateById(game);
        }
        return result;
    }

    @Override
    public void settleOpportunityTask(OpportunitySettleReq req)  {
        Game game = getById(req.getGameId());
        if (game == null) {
            throw new CustomException("游戏不存在");
        }
        if (game.getStage() != 1) {
            throw new CustomException("当前阶段不能开始任务");
        }
        if(game.getChessRound() < 3){
            throw new CustomException("当前轮次不能开始机会宝地任务");
        }
        if(game.getChessPhase() != 3){
            throw new CustomException("当前阶段不能开始任务");
        }
        TeamTileAction teamTileAction = teamTileActionService
                .lambdaQuery()
                .eq(TeamTileAction::getGameId, req.getGameId())
                .eq(TeamTileAction::getTeamId, req.getTeamId())
                .eq(TeamTileAction::getRound, game.getChessRound())
                .eq(TeamTileAction::getPhase, 2)
                .one();
        if (teamTileAction == null) {
            throw new CustomException("未找到该队伍的格子记录");
        }
        teamTileAction.setOpportunityTiles(null);
        teamTileActionService.updateById(teamTileAction);
        if(Boolean.TRUE.equals(req.getSuccess())){
            TileOccupyReq occupyReq = new TileOccupyReq();
            occupyReq.setGameId(req.getGameId());
            occupyReq.setTeamId(req.getTeamId());
            occupyReq.setTileIds(req.getRewardTileIds());
            occupyReq.setTriggerBlindBox(req.getTriggerBlindBox());
            occupyReq.setBlindBoxTileIds(req.getBlindBoxTileIds());
            occupyReq.setTriggerGoldCenter(req.getTriggerGoldCenter());
            try {
                occupy(occupyReq,3);
            } catch (JsonProcessingException e) {
                throw new CustomException(e);
            }
        }
    }

    @Override
    public void settleFortressBattle(FortressBattleReq req) throws JsonProcessingException {
        Game game = getById(req.getGameId());
        if (game == null) {
            throw new CustomException("游戏不存在");
        }
        if (game.getStage() != 1) {
            throw new CustomException("当前阶段不合法");
        }
        if (!Objects.equals(game.getChessPhase(), 3)) {
            throw new CustomException("当前小阶段不合法");
        }
        Long winnerId = req.getWinnerTeamId();
        Long loserId = Objects.equals(req.getAttackerTeamId(), winnerId)
                ? req.getDefenderTeamId() : req.getAttackerTeamId();
        // 1. 发起方移除效果格子
        removeTileFromTeamAction(req.getGameId(), req.getAttackerTeamId(), game.getChessRound(), req.getTileId(), "fortress");
        // 2. 获取败者格子并选一半
        List<TileWithSource> toTransfer = collectHalfOwnedTiles(req.getGameId(), loserId);
        if (toTransfer.isEmpty()) throw new CustomException("败者无可转移领地");
        // 3. 移除格子
        List<TeamTileAction> loserActions = teamTileActionService.lambdaQuery()
                .eq(TeamTileAction::getGameId, req.getGameId())
                .eq(TeamTileAction::getTeamId, loserId)
                .eq(TeamTileAction::getPhase, 2)
                .list();
        removeTilesFromTeamActions(loserActions, toTransfer);
        // 4. 加给胜者
        addTilesToWinnerAction(req.getGameId(), winnerId, toTransfer.stream().map(TileWithSource::tileId).toList());
        // 5. 保存
        teamTileActionService.updateBatchById(loserActions);
    }

    @Override
    public void settleBlindBoxEvent(BlindBoxSettleReq req) {
        Game game = getById(req.getGameId());
        if (game == null) {
            throw new CustomException("游戏不存在");
        }
        if (game.getStage() != 1) {
            throw new CustomException("当前阶段不合法");
        }
        if (!Objects.equals(game.getChessPhase(), 3)) {
            throw new CustomException("当前小阶段不合法");
        }
        if (req.getInvolvedTeamIds() == null || req.getInvolvedTeamIds().size() != 3) {
            throw new CustomException("参与队伍数量应为3");
        }
        if (!req.getInvolvedTeamIds().contains(req.getWinnerTeamId())) {
            throw new CustomException("胜利方不在参与队伍中");
        }
        // 1. 移除盲盒格子
        try {
            removeTileFromTeamAction(req.getGameId(), req.getTeamId(), game.getChessRound(), req.getTileId(), "blindBox");
        } catch (JsonProcessingException e) {
            throw new CustomException("处理盲盒格子时出错");
        }
        // 2. 筛出失败方
        List<Long> loserIds = req.getInvolvedTeamIds().stream()
                .filter(id -> !id.equals(req.getWinnerTeamId()))
                .toList();

        // 3. 从两败者中各收集一半领地
        List<TileWithSource> toTransfer = new ArrayList<>();
        for (Long loserId : loserIds) {
            List<TileWithSource> tiles = collectHalfOwnedTiles(req.getGameId(), loserId);
            toTransfer.addAll(tiles);
            // 删除这些格子从败者记录中
            List<TeamTileAction> loserActions = teamTileActionService.lambdaQuery()
                    .eq(TeamTileAction::getGameId, req.getGameId())
                    .eq(TeamTileAction::getTeamId, loserId)
                    .eq(TeamTileAction::getPhase, 2)
                    .list();
            removeTilesFromTeamActions(loserActions, tiles);
            teamTileActionService.updateBatchById(loserActions);
        }
        // 4. 加给胜者
        addTilesToWinnerAction(req.getGameId(), req.getWinnerTeamId(), toTransfer.stream().map(TileWithSource::tileId).toList());
    }

    @Override
    public List<TeamScoreRankResp> getStudentRank(Long id) {
            Game game = getById(id);
            if(game == null){
                throw new CustomException("游戏不存在");
            }
            Integer round = game.getChessRound();
            List<TeamScoreLog> logs = teamScoreLogService.lambdaQuery()
                    .eq(TeamScoreLog::getGameId, id)
                    .eq(TeamScoreLog::getRound, round)
                    .eq(TeamScoreLog::getPhase, 1)
                    .eq(TeamScoreLog::getReason, 3)
                    .orderByDesc(TeamScoreLog::getScore)
                    .list();
        // 获取所有涉及到的小组 ID
        Set<Long> teamIds = logs.stream().map(TeamScoreLog::getTeamId).collect(Collectors.toSet());
        Map<Long, Team> teamMap = teamService.lambdaQuery()
                .in(Team::getId, teamIds)
                .eq(Team::getGameId, game.getId())
                .list()
                .stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));
        return logs.stream().map(log1 -> {
            TeamScoreRankResp resp = new TeamScoreRankResp();
            resp.setTeamId(log1.getTeamId());
            resp.setThisRoundScore(log1.getScore());
            resp.setSubmitTime(log1.getSubmitTime());
            Team team = teamMap.get(log1.getTeamId());
            if (team != null) {
                resp.setTeamName(team.getLeaderName());
            }
            return resp;
        }).toList();
    }

    @Override
    public void updateScore(ScoreUpdateReq req) {
        Integer type = req.getType();
        Integer stage = req.getStage();
        Long id = req.getId();
        Long gameId = req.getGameId();
        Integer num = req.getNum();
        String comment = req.getComment();
        Game game = Optional.ofNullable(getById(gameId))
                .orElseThrow(() -> new CustomException("游戏不存在"));
        if (type == 1) {
            // === 小组加分 ===
            Team team = Optional.ofNullable(teamService.lambdaQuery()
                    .eq(Team::getId, id)
                    .eq(Team::getGameId, gameId)
                    .one()).orElseThrow(() -> new CustomException("小组不存在"));
            if (stage == 1) {
                team.setBoardScoreAdjusted(team.getBoardScoreAdjusted() + num);
            } else if (stage == 2) {
                team.setProposalScoreAdjusted(team.getProposalScoreAdjusted() + num);
            }
            teamService.updateById(team);
            teamScoreLogService.save(TeamScoreLog.createLog(team.getId(), gameId, num, stage, game.getChessRound(), comment));
        } else {
            // === 个人加分 ===
            TeamMember member = Optional.ofNullable(teamMemberService.getById(id))
                    .orElseThrow(() -> new CustomException("成员不存在"));

            member.setIndividualScore(member.getIndividualScore() + num);
            teamMemberService.updateById(member);

            Long teamId = member.getTeamId();
            Team team = Optional.ofNullable(teamService.lambdaQuery()
                    .eq(Team::getId, teamId)
                    .eq(Team::getGameId, gameId)
                    .one()).orElseThrow(() -> new CustomException("成员所属小组不存在"));

            int maxCount = game.getTeamMemberCount();
            List<TeamMember> members = teamMemberService.lambdaQuery()
                    .eq(TeamMember::getTeamId, teamId)
                    .orderByDesc(TeamMember::getIndividualScore)
                    .last("LIMIT " + maxCount)
                    .list();

            int totalScore = members.stream().mapToInt(TeamMember::getIndividualScore).sum();
            team.setMemberScoreSum(totalScore);
            teamService.updateById(team);
            studentScoreLogService.save(StudentScoreLog.createLog(member.getStudentId(), teamId, gameId, num, stage, game.getChessRound(), comment));
        }
    }

    private void addTilesToWinnerAction(Long gameId, Long teamId, List<Integer> newTiles) {
        Game game = getById(gameId);
        TeamTileAction winnerAction = teamTileActionService.lambdaQuery()
                .eq(TeamTileAction::getGameId, gameId)
                .eq(TeamTileAction::getTeamId, teamId)
                .eq(TeamTileAction::getPhase, 2)
                .eq(TeamTileAction::getRound, game.getChessRound())
                .one();
        if (winnerAction == null) throw new CustomException("胜者数据缺失");
        winnerAction.setAllTiles(mergeTiles(winnerAction.getAllTiles(), newTiles));
        winnerAction.setSettledTileCount(mergeTilesToCount(winnerAction.getAllTiles()));
        boolean updated = teamTileActionService.updateById(winnerAction);
        if (!updated) {
            throw new CustomException("更新胜者格子数据失败");
        }
    }
    private void removeTilesFromTeamActions(List<TeamTileAction> actions, List<TileWithSource> toRemove) {
        Map<Long, List<Integer>> removeMap = new HashMap<>();
        for (TileWithSource t : toRemove) {
            removeMap.computeIfAbsent(t.actionId(), k -> new ArrayList<>()).add(t.tileId());
        }

        Map<Long, TeamTileAction> actionMap = actions.stream()
                .collect(Collectors.toMap(TeamTileAction::getId, t -> t));

        for (Map.Entry<Long, List<Integer>> entry : removeMap.entrySet()) {
            TeamTileAction action = actionMap.get(entry.getKey());
            List<Integer> tiles = parseTiles(action.getAllTiles());
            tiles.removeAll(entry.getValue());
            action.setAllTiles(joinTiles(tiles));
            action.setSettledTileCount(tiles.size());
        }
    }
    private List<TileWithSource> collectHalfOwnedTiles(Long gameId, Long teamId) {
        List<TeamTileAction> actions = teamTileActionService.lambdaQuery()
                .eq(TeamTileAction::getGameId, gameId)
                .eq(TeamTileAction::getTeamId, teamId)
                .eq(TeamTileAction::getPhase, 2)
                .list();

        List<TileWithSource> allTiles = new ArrayList<>();
        for (TeamTileAction action : actions) {
            List<Integer> tiles = parseTiles(action.getAllTiles());
            for (Integer tile : tiles) {
                allTiles.add(new TileWithSource(tile, action.getId()));
            }
        }
        int start = allTiles.size() / 2;
        return allTiles.subList(start, allTiles.size());
    }

    private void removeTileFromTeamAction(Long gameId, Long teamId, Integer round, Integer tileId, String type) throws JsonProcessingException {
        TeamTileAction action = teamTileActionService.lambdaQuery()
                .eq(TeamTileAction::getGameId, gameId)
                .eq(TeamTileAction::getTeamId, teamId)
                .eq(TeamTileAction::getPhase, 2)
                .eq(TeamTileAction::getRound, round)
                .one();

        if (action == null) throw new CustomException("指定轮次格子记录不存在");

        ObjectMapper mapper = new ObjectMapper();
        switch (type) {
            case "fortress" -> {
                String json = Optional.ofNullable(action.getFortressTiles()).orElse("[]");
                List<BoardInitReq.FortressTile> raw = mapper.readValue(json, new TypeReference<>() {});
                List<BoardInitReq.FortressTile> list = new ArrayList<>(raw);
                list.removeIf(f -> f.getTileId().equals(tileId));
                action.setFortressTiles(mapper.writeValueAsString(list));
            }
            case "blindBox" -> {
                String json = Optional.ofNullable(action.getBlindBoxTiles()).orElse("[]");
                List<BoardInitReq.BlindBoxTile> raw = mapper.readValue(json, new TypeReference<>() {});
                List<BoardInitReq.BlindBoxTile> list = new ArrayList<>(raw);
                list.removeIf(b -> b.getTileId().equals(tileId));
                action.setBlindBoxTiles(mapper.writeValueAsString(list));
            }
            case "gold" -> {
                List<Integer> list = new ArrayList<>(parseTiles(action.getGoldCenterTiles()));
                list.removeIf(id -> id.equals(tileId));
                action.setGoldCenterTiles(joinTiles(list));
            }
            case "opportunity" -> {
                List<Integer> list = new ArrayList<>(parseTiles(action.getOpportunityTiles()));
                list.removeIf(id -> id.equals(tileId));
                action.setOpportunityTiles(joinTiles(list));
            }
            default -> throw new CustomException("未知格子类型：" + type);
        }
        teamTileActionService.updateById(action);
    }


    // 工具：把List<Integer>拼成逗号分隔字符串
    private String joinTiles(List<Integer> tiles) {
        if (tiles == null || tiles.isEmpty()) {
            return "";
        }
        return tiles.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private List<Integer> parseTiles(String tilesStr) {
        if (tilesStr == null || tilesStr.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(tilesStr.split(","))
                .map(String::trim)
                .map(Integer::valueOf)
                .collect(Collectors.toCollection(ArrayList::new));
    }


    private void validateGameState(Game game) {
        if (game == null) throw new CustomException("游戏不存在");
        if (game.getStatus() != 1) log.warn("游戏已结束");
        if (game.getStage() != 1 || game.getChessPhase() != 0) {
            throw new CustomException("当前不可进行该操作");
        }
    }
    @Override
    public List<XxtStudentScoreExcelDTO> validateAndParseFile(MultipartFile file) {
        XxtStudentScoreListener listener = new XxtStudentScoreListener();
        try {
            EasyExcelFactory.read(file.getInputStream(), listener)
                    .head(XxtStudentScoreExcelDTO.class)
                    .sheet()
                    .headRowNumber(6)
                    .doRead();
        } catch (ExcelReadStopException e) {
            log.info("读取中断: {}", e.getMessage());
        } catch (IOException e) {
            throw new CustomException(CodeEnum.SYSTEM_ERROR);
        }
        List<XxtStudentScoreExcelDTO> scores = listener.getData();
        if (CollectionUtils.isEmpty(scores)) throw new CustomException("上传数据为空");
        return scores;
    }
    @Override
    public Map<Long, List<XxtStudentScoreExcelDTO>> buildTeamGroupMap(List<XxtStudentScoreExcelDTO> scores, Long gameId, Long cid) {
        Set<String> snos = scores.stream()
                .map(XxtStudentScoreExcelDTO::getSno)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        // 学号 -> 学生
        Map<String, ClassStudent> snoToStudent = classStudentService.lambdaQuery()
                .in(ClassStudent::getSno, snos)
                .eq(ClassStudent::getCid, cid)
                .list().stream().collect(Collectors.toMap(ClassStudent::getSno, s -> s));

        // 学生ID -> 小组ID
        Map<Long, Long> studentIdToTeamId = teamMemberService.lambdaQuery()
                .eq(TeamMember::getGameId, gameId)
                .list().stream()
                .collect(Collectors.toMap(TeamMember::getStudentId, TeamMember::getTeamId));

        Map<Long, List<XxtStudentScoreExcelDTO>> groupMap = new HashMap<>();
        for (XxtStudentScoreExcelDTO dto : scores) {
            ClassStudent student = snoToStudent.get(dto.getSno());
            if (student == null) continue;
            Long teamId = studentIdToTeamId.get(student.getId());
            if (teamId == null) continue;
            groupMap.computeIfAbsent(teamId, k -> new ArrayList<>()).add(dto);
        }
        return groupMap;
    }

    private Map<Long, Team> getTeamMapByGame(Long gameId) {
        return teamService.lambdaQuery()
                .eq(Team::getGameId, gameId)
                .list().stream().collect(Collectors.toMap(Team::getId, t -> t));
    }

    /**
     * 计算本轮每个小组得分，更新累计得分，并根据本轮得分及最晚提交时间排序返回排行榜
     */
    @Override
    public List<TeamScoreRankResp> calculateAndUpdateTeamScoresAndGetRankResp(
            Map<Long, List<XxtStudentScoreExcelDTO>> groupMap,
            Game game,
            Map<Long, Team> teamMap) {
        int maxCount = game.getTeamMemberCount();
        List<TeamScoreRankResp> resultList = new ArrayList<>();
        List<ScoreUpdateDTO> updateList = new ArrayList<>();
        for (Map.Entry<Long, List<XxtStudentScoreExcelDTO>> entry : groupMap.entrySet()) {
            Long teamId = entry.getKey();
            List<XxtStudentScoreExcelDTO> teamScores = entry.getValue();
            // 计算 topN 分数总和
            List<Integer> topScores = teamScores.stream()
                    .map(dto -> {
                        try {
                            return Integer.parseInt(dto.getScore());
                        } catch (NumberFormatException e) {
                            return 0;
                        }
                    })
                    .sorted(Comparator.reverseOrder())
                    .limit(maxCount)
                    .toList();

            int thisRoundScore = topScores.stream().mapToInt(Integer::intValue).sum();

            // 获取最新提交时间
            LocalDateTime latestTime = teamScores.stream()
                    .map(XxtStudentScoreExcelDTO::getTime)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.MIN);

            // 更新 Team 总分
            Team team = teamMap.get(teamId);
            if (team != null) {
                int oldScore = Optional.ofNullable(team.getMemberScoreSum()).orElse(0);
                team.setMemberScoreSum(oldScore + thisRoundScore);
                // 更新数据库
                teamService.lambdaUpdate()
                        .eq(Team::getId, team.getId())
                        .eq(Team::getGameId, team.getGameId())
                        .update(team);
                // 构造排名返回对象
                TeamScoreRankResp resp = new TeamScoreRankResp();
                resp.setTeamId(teamId);
                resp.setTeamName(team.getLeaderName());
                resp.setThisRoundScore(thisRoundScore);
                resp.setSubmitTime(latestTime);
                resultList.add(resp);
            }
            // 收集成员得分，用于批量更新
            for (XxtStudentScoreExcelDTO dto : teamScores) {
                if (org.apache.commons.lang3.StringUtils.isNotBlank(dto.getSno())) {
                    ScoreUpdateDTO param = new ScoreUpdateDTO();
                    param.setSno(dto.getSno());
                    try {
                        param.setAddScore(Integer.parseInt(dto.getScore()));
                    } catch (NumberFormatException e) {
                        param.setAddScore(0);
                    }
                    updateList.add(param);
                }
            }
        }
        resultList.sort(Comparator.comparingInt(TeamScoreRankResp::getThisRoundScore).reversed()
                .thenComparing(TeamScoreRankResp::getSubmitTime));
        if (!updateList.isEmpty()) {
            for (ScoreUpdateDTO dto : updateList) {
                teamMemberMapper.addScore(dto.getSno(), game.getId(), dto.getAddScore());
            }
        }

        return resultList;
    }

    @Override
    public void addComment(CommentReq req) {
        Game game = getById(req.getGameId());
        if(game == null){
            throw new CustomException("未找到游戏信息");
        }
        TeamMember member = teamMemberService.lambdaQuery()
                .eq(TeamMember::getGameId, req.getGameId())
                .eq(TeamMember::getStudentId, req.getStudentId())
                .one();
        if(member == null){
            throw new CustomException("未找到学生信息");
        }
        StudentScoreLog studentScoreLog = new StudentScoreLog();
        studentScoreLog.setStudentId(member.getStudentId());
        studentScoreLog.setTeamId(member.getTeamId());
        studentScoreLog.setGameId(req.getGameId());
        studentScoreLog.setScore(0);
        studentScoreLog.setReason(4);
        studentScoreLog.setPhase(game.getStage());
        studentScoreLog.setRound(game.getChessRound());
        studentScoreLog.setComment(req.getComment());
        studentScoreLogService.save(studentScoreLog);
    }

    @Override
    public void outTeam(OutReq req) {
        Game game = getById(req.getGameId());
        if(game.getStage() < 2 || game.getProposalStage() > 2){
            throw new CustomException("当前不允许淘汰队伍");
        }
        if(req.getType() < 1 || req.getType() > 2){
            throw new CustomException("type参数错误");
        }
        for(Long id : req.getTeamIds()){
            teamService.lambdaUpdate()
                    .eq(Team::getGameId, req.getGameId())
                    .eq(Team::getId, id)
                    .set(Team::getAlive,req.getType())
                    .update();
        }
    }


    private List<TeamInfoDTO> parseExcel(MultipartFile file) {
        List<TeamInfoDTO> groupList = new ArrayList<>();
        AtomicLong teamSeq = new AtomicLong(1);
        Set<Long> existingTeamNos = new HashSet<>();
        try (InputStream inputStream = file.getInputStream()) {
            EasyExcelFactory.read(inputStream, new AnalysisEventListener<Map<Integer, String>>() {
                private int rowIndex = 0;
                @Override
                public void invoke(Map<Integer, String> row, AnalysisContext context) {
                    rowIndex++;
                    if (rowIndex == 0) return; // 跳过表头
                    try {
                        handleRow(row, rowIndex, groupList, teamSeq, existingTeamNos);
                    } catch (Exception e) {
                        log.warn("第 {} 行解析失败，跳过该行，错误信息：{}", rowIndex, e.getMessage());
                    }
                }
                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("Excel解析完成：共 {} 条小组", groupList.size());
                }
            }).sheet().doRead();
        } catch (IOException e) {
            throw new CustomException("读取Excel失败");
        }
        return groupList;
    }

    private void handleRow(Map<Integer, String> row, int rowIndex,
                           List<TeamInfoDTO> groupList, AtomicLong teamSeq,
                           Set<Long> existingTeamNos) {
        try {
            Long teamNo = parseTeamNo(row.getOrDefault(0, "").trim(), teamSeq, existingTeamNos);
            if (teamNo == null) {
                log.warn("第 {} 行跳过：组号无法解析且分配失败", rowIndex);
                return;
            }
            if (existingTeamNos.contains(teamNo)) {
                while (existingTeamNos.contains(teamSeq.get())) {
                    teamSeq.incrementAndGet();
                }
                teamNo = teamSeq.getAndIncrement();
            }
            existingTeamNos.add(teamNo);
            MemberDTO leader = parseLeader(row);
            if (leader == null) {
                log.warn("第 {} 行跳过：组长信息不完整", rowIndex);
                return;
            }
            List<MemberDTO> members = parseMembers(row);
            if (members.isEmpty()) {
                log.warn("第 {} 行跳过：成员信息不完整", rowIndex);
                return;
            }
            groupList.add(new TeamInfoDTO(teamNo, leader, members));
        } catch (Exception e) {
            log.warn("第 {} 行解析异常，跳过该行，错误信息：{}", rowIndex, e.getMessage());
        }
    }

    private Long parseTeamNo(String teamNoStr, AtomicLong teamSeq, Set<Long> existingTeamNos) {
        if (!teamNoStr.isEmpty()) {
            try {
                return Long.parseLong(teamNoStr);
            } catch (NumberFormatException e) {
                // 格式非法，尝试自动分配
            }
        }
        // 自动分配不重复小组号
        while (existingTeamNos.contains(teamSeq.get())) {
            teamSeq.incrementAndGet();
        }
        return teamSeq.getAndIncrement();
    }

    private MemberDTO parseLeader(Map<Integer, String> row) {
        String leaderName = row.getOrDefault(1, "").trim();
        String leaderId = row.getOrDefault(2, "").trim();
        if (leaderName.isEmpty() || leaderId.isEmpty()) {
            return null;
        }
        return new MemberDTO(leaderName, leaderId);
    }

    private List<MemberDTO> parseMembers(Map<Integer, String> row) {
        List<MemberDTO> members = new ArrayList<>();
        int colIndex = 3;
        while (true) {
            String name = row.get(colIndex);
            String id = row.get(colIndex + 1);
            boolean nameEmpty = (name == null || name.trim().isEmpty());
            boolean idEmpty = (id == null || id.trim().isEmpty());
            if (nameEmpty && idEmpty) {
                break;
            }
            if (nameEmpty || idEmpty) {
                return Collections.emptyList();
            }
            members.add(new MemberDTO(name.trim(), id.trim()));
            colIndex += 2;
        }
        return members;
    }


    private Game createGame(GameInitReq req) {
        Game game = new Game()
                .setCid(req.getCid())
                .setStudentCount(req.getStudentNum())
                .setTeamCount(req.getTeamNum())
                .setTeamMemberCount(req.getTeamMemberCount())
                .setStage(0)
                .setChessRound(0)
                .setChessPhase(0)
                .setProposalStage(0)
                .setProposalRound(0)
                .setLastSavedAt(new Date())
                .setStatus(1);
        save(game);
        return game;
    }
    private List<ClassStudent> collectMissingStudents(List<TeamInfoDTO> groupList,
                                                      Map<String, ClassStudent> map,
                                                      Long cid) {
        List<ClassStudent> list = new ArrayList<>();
        // 学号集合
        Set<String> exists = new HashSet<>(map.keySet());
        for (TeamInfoDTO dto : groupList) {
            MemberDTO leader = dto.getLeader();
            if (leader != null && !exists.contains(leader.getId())) {
                list.add(newStudent(leader, cid));
            }
            for (MemberDTO member : dto.getMembers()) {
                if (member != null && !exists.contains(member.getId())) {
                    list.add(newStudent(member, cid));
                }
            }
        }
        return list;
    }

    private ClassStudent newStudent(MemberDTO member, Long cid) {
        ClassStudent student = new ClassStudent();
        student.setName(member.getName());
        student.setSno(member.getId());
        student.setCid(cid);
        // 其他初始化
        return student;
    }

    private TeamUploadDTO saveTeamsAndMembers(List<TeamInfoDTO> groupList,
                                              Game game,
                                              Map<String, ClassStudent> map) {

        List<Team> teamList = new ArrayList<>();
        List<TeamMember> memberList = new ArrayList<>();
        for (TeamInfoDTO dto : groupList) {
            MemberDTO leaderMember = dto.getLeader();
            ClassStudent leader = (leaderMember != null) ? map.get(leaderMember.getId()) : null;
            if (leaderMember == null || leader == null) {
                continue;
            }
            Team team = new Team();
            team.setId(dto.getTeamNo());// 这里一定要修改,不然。。。。。
            team.setGameId(game.getId());
            team.setLeaderId(leader.getId());
            team.setLeaderName(leader.getName());
            team.setTotalMembers(1 + dto.getMembers().size());
            team.setAlive(1);
            team.setSno(leader.getSno());
            teamList.add(team);
        }
        teamService.saveBatch(teamList);
        for (int i = 0; i < teamList.size(); i++) {
            Team team = teamList.get(i);
            TeamInfoDTO dto = groupList.get(i);
            MemberDTO leaderMember = dto.getLeader();
            if (leaderMember != null) {
                ClassStudent leader = map.get(leaderMember.getId());
                if (leader != null) {
                    memberList.add(createMember(team.getId(), leader, game.getId(), true));
                }
            }
            for (MemberDTO member : dto.getMembers()) {
                if (member != null) {
                    ClassStudent student = map.get(member.getId());
                    if (student != null) {
                        memberList.add(createMember(team.getId(), student,game.getId(), false));
                    } else {
                        log.warn("学生 {} 找不到，跳过", member.getName());
                    }
                }
            }
        }
        teamMemberService.saveBatch(memberList);
        return new TeamUploadDTO(teamList.size(), memberList.size());
    }

    private TeamMember createMember(Long teamId, ClassStudent student,Long gameId, boolean isLeader) {
        TeamMember m = new TeamMember();
        m.setTeamId(teamId);
        m.setStudentId(student.getId());
        m.setStudentName(student.getName());
        m.setGameId(gameId);
        m.setIsLeader(isLeader ? 1 : 0);
        m.setIndividualScore(0);
        m.setSno(student.getSno());
        return m;
    }
    private TeamUploadResp buildUploadResp(GameInitReq req, int size,TeamUploadDTO dto, Long id) {
        TeamUploadResp resp = new TeamUploadResp();
        resp.setTeamNum(req.getTeamNum());
        resp.setStudentNum(req.getStudentNum());
        resp.setSuccessTeamNum(dto.getSuccessTeamNum());
        resp.setFailTeamNum(req.getTeamNum() - dto.getSuccessTeamNum());
        resp.setSuccessStudentNum(dto.getSuccessStudentNum());
        resp.setFailStudentNum(req.getStudentNum() - dto.getSuccessStudentNum());
        resp.setAddStudentNum(size);
        resp.setGameId(id);
        return resp;
    }
    private Map<String, ClassStudent> getClassStudentMap(Long cid) {
        List<ClassStudent> list = classStudentService
                .lambdaQuery()
                .select(ClassStudent::getId, ClassStudent::getName, ClassStudent::getSno)
                .eq(ClassStudent::getCid, cid)
                .eq(ClassStudent::getIsDeleted,0)
                .list();
        return list.stream().collect(Collectors.toMap(ClassStudent::getSno, classStudent -> classStudent));
    }
    private String mergeTiles(String oldTilesStr, List<Integer> newTiles) {
        Set<Integer> set = new LinkedHashSet<>();
        if (StringUtils.hasText(oldTilesStr)) {
            Arrays.stream(oldTilesStr.split(","))
                    .map(String::trim)
                    .map(Integer::valueOf)
                    .forEach(set::add);
        }
        if (newTiles != null) {
            set.addAll(newTiles);
        }
        return set.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private int mergeTilesToCount(String mergedTilesStr) {
        if (!StringUtils.hasText(mergedTilesStr)) return 0;
        return (int) Arrays.stream(mergedTilesStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .count();
    }

    private <T> String mergeJsonList(String oldJson, List<T> newList, Function<T, Integer> idGetter, Class<T> clazz) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        List<T> oldList = new ArrayList<>();
        if (StringUtils.hasText(oldJson)) {
            oldList = mapper.readValue(oldJson, mapper.getTypeFactory().constructCollectionType(List.class, clazz));
        }
        Map<Integer, T> map = new LinkedHashMap<>();
        for (T item : oldList) {
            map.put(idGetter.apply(item), item);
        }
        for (T item : newList) {
            map.put(idGetter.apply(item), item);
        }
        return mapper.writeValueAsString(new ArrayList<>(map.values()));
    }

}




