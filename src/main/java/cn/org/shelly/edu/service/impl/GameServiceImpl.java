package cn.org.shelly.edu.service.impl;

import cn.org.shelly.edu.controller.GameController;
import cn.org.shelly.edu.enums.CodeEnum;
import cn.org.shelly.edu.exception.CustomException;
import cn.org.shelly.edu.exception.ExcelReadStopException;
import cn.org.shelly.edu.listener.XxtStudentScoreListener;
import cn.org.shelly.edu.mapper.GameMapper;
import cn.org.shelly.edu.model.dto.MemberDTO;
import cn.org.shelly.edu.model.dto.TeamInfoDTO;
import cn.org.shelly.edu.model.dto.TeamUploadDTO;
import cn.org.shelly.edu.model.dto.XxtStudentScoreExcelDTO;
import cn.org.shelly.edu.model.pojo.*;
import cn.org.shelly.edu.model.req.AssignReq;
import cn.org.shelly.edu.model.req.GameInitReq;
import cn.org.shelly.edu.model.req.TileOccupyReq;
import cn.org.shelly.edu.model.resp.TeamRankResp;
import cn.org.shelly.edu.model.resp.TeamScoreRankResp;
import cn.org.shelly.edu.model.resp.TeamUploadResp;
import cn.org.shelly.edu.model.resp.UnselectedTeamResp;
import cn.org.shelly.edu.service.*;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
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

    @Override
    public TeamUploadResp init(GameInitReq req) {
        Game game = createGame(req);
        List<TeamInfoDTO> groupList = parseExcel(req.getFile());
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
        classService.lambdaUpdate()
                .eq(Classes::getId, req.getCid())
                .setSql("current_students = current_students + " + studentMap.size())
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
        Map<Long, List<XxtStudentScoreExcelDTO>> groupMap = buildTeamGroupMap(scores, gameId);
        // 4. 获取该游戏所有小组映射：teamId -> Team
        Map<Long, Team> teamMap = getTeamMapByGame(gameId);
        // 5. 计算本轮小组得分，更新累计得分，并生成排行榜响应
        List<TeamScoreRankResp> rankList = calculateAndUpdateTeamScoresAndGetRankResp(groupMap, game, teamMap);
        // 6. 更新游戏阶段等信息
        game.setChessPhase(1);
        game.setStage(1);
        game.setLastSavedAt(new Date());
        updateById(game);
        // 7. 返回本轮小组得分排名列表
        return rankList;
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
        updateById(game);
    }

    @Override
    public List<UnselectedTeamResp> getUnselectedTeamsByGame(Long gameId) {
        Game game = getById(gameId);
        if (game == null) {
            throw new CustomException("游戏不存在");
        }
        if(game.getChessPhase() != 2){
            throw new CustomException("当前不是走棋阶段");
        }
        Map<Long, Team> teams = teamService.lambdaQuery()
                .eq(Team::getGameId, gameId)
                .list()
                .stream()
                .collect(Collectors.toMap(Team::getId, team -> team));
        // 查询当前轮次未选择的小组的格子行动记录
        List<TeamTileAction> unselectedActions = teamTileActionService.lambdaQuery()
                .eq(TeamTileAction::getGameId, gameId)
                .eq(TeamTileAction::getRound, game.getChessRound())
                .eq(TeamTileAction::getSelected, 0)
                .list();
        // 组装返回结果
        return unselectedActions.stream()
                .map(action -> {
                    Team team = teams.get(action.getTeamId());
                    UnselectedTeamResp resp = new UnselectedTeamResp();
                    resp.setTeamId(action.getTeamId());
                    if (team != null) {
                        resp.setLeaderName(team.getLeaderName());
                        resp.setSno(team.getSno());
                        resp.setAssignCount(action.getOriginalTileCount());
                        resp.setLeaderName(team.getLeaderName());
                            resp.setLeaderId(team.getLeaderId());
                    }
                    return resp;
                })
                .toList();
    }

    @Override
    public Boolean occupy(TileOccupyReq req) {
        Game game = getById(req.getGameId());
        if (game == null) {
            throw new CustomException("游戏不存在");
        }
        if (game.getStage() != 1) {
            throw new CustomException("当前不是棋盘赛阶段");
        }
        if (game.getChessPhase() != 2) {
            throw new CustomException("当前不是上传领地阶段");
        }
        BoardConfig config = boardConfigService.lambdaQuery()
                .eq(BoardConfig::getGameId, req.getGameId())
                .one();
        if (config == null) {
            throw new CustomException("游戏配置不存在");
        }
        Set<Integer> blindBoxSet = parseTiles(config.getBlindBoxTiles());
        Set<Integer> fortressSet = parseTiles(config.getFortressTiles());
        Set<Integer> goldCenterSet = parseTiles(config.getGoldCenterTiles());
        Set<Integer> opportunitySet = parseTiles(config.getOpportunityTiles());
        // 前端传入的本轮选择的所有格子id
        List<Integer> selectedTiles = req.getTileIds();
        if (selectedTiles == null || selectedTiles.isEmpty()) {
            throw new CustomException("请选择至少一个格子");
        }
        // 分类过滤
        List<Integer> blindBoxTiles = new ArrayList<>();
        List<Integer> fortressTiles = new ArrayList<>();
        List<Integer> goldCenterTiles = new ArrayList<>();
        List<Integer> opportunityTiles = new ArrayList<>();
        for (Integer tileId : selectedTiles) {
            if (blindBoxSet.contains(tileId)) {
                blindBoxTiles.add(tileId);
            }
            if (fortressSet.contains(tileId)) {
                fortressTiles.add(tileId);
            }
            if (goldCenterSet.contains(tileId)) {
                goldCenterTiles.add(tileId);
            }
            if (opportunitySet.contains(tileId)) {
                opportunityTiles.add(tileId);
            }
        }
        String blindBoxStr = joinTiles(blindBoxTiles);
        String fortressStr = joinTiles(fortressTiles);
        String goldCenterStr = joinTiles(goldCenterTiles);
        String opportunityStr = joinTiles(opportunityTiles);
        String allTilesStr = joinTiles(selectedTiles);
        TeamTileAction action = new TeamTileAction();
        action.setGameId(req.getGameId());
        action.setTeamId(req.getTeamId());
        // 这里需要获取当前游戏的轮次，假设从game里拿
        action.setRound(game.getChessRound());
        action.setPhase(1); // 选格子阶段
        action.setAllTiles(allTilesStr);
        action.setBlindBoxTiles(blindBoxStr);
        action.setFortressTiles(fortressStr);
        action.setGoldCenterTiles(goldCenterStr);
        action.setOpportunityTiles(opportunityStr);
        // 结算格子数这里先等于原始数量，后续如果有扣除逻辑，再修改
        action.setSettledTileCount(selectedTiles.size());
        action.setSelected(1);
        action.setGmtCreate(new Date());
        action.setGmtUpdate(new Date());
        boolean saved = teamTileActionService.save(action);
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
        // 按 teamId 聚合 totalTile（结算后剩余格子数）
        Map<Long, Integer> teamToTotalTile = new HashMap<>();
        for (TeamTileAction action : actions) {
            int settledCount = Optional.ofNullable(action.getSettledTileCount()).orElse(0);
            teamToTotalTile.merge(action.getTeamId(), settledCount, Integer::sum);
        }
        // 查询游戏所有小组
        List<Team> teams = teamService.lambdaQuery()
                .eq(Team::getGameId, game.getId())
                .list();
        // 组装返回结果
        List<TeamRankResp> rankList = new ArrayList<>();
        for (Team team : teams) {
            TeamRankResp resp = new TeamRankResp();
            resp.setTeamId(team.getId());
            resp.setLeaderName(team.getLeaderName());
            resp.setTotalScore(Optional.ofNullable(team.getMemberScoreSum()).orElse(0));
            resp.setTotalTile(teamToTotalTile.getOrDefault(team.getId(), 0));
            resp.setLeaderSno(team.getSno());
            rankList.add(resp);
        }
        // 按总领地数降序排序（可根据需求调整排序规则）
        rankList.sort(Comparator.comparingInt(TeamRankResp::getTotalTile).reversed());
        return rankList;
    }

    // 工具：解析逗号分隔字符串为Set<Integer>
    private Set<Integer> parseTiles(String tilesStr) {
        if (!StringUtils.hasText(tilesStr)) {
            return Collections.emptySet();
        }
        return Arrays.stream(tilesStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
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


    private void validateGameState(Game game) {
        if (game == null) throw new CustomException("游戏不存在");
        if (game.getStatus() != 1) log.warn("游戏已结束");
        if (game.getStage() != 1 || game.getChessPhase() != 0) {
            throw new CustomException("当前不可进行该操作");
        }
    }

    private List<XxtStudentScoreExcelDTO> validateAndParseFile(MultipartFile file) {
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

    private Map<Long, List<XxtStudentScoreExcelDTO>> buildTeamGroupMap(List<XxtStudentScoreExcelDTO> scores, Long gameId) {
        Set<String> snos = scores.stream()
                .map(XxtStudentScoreExcelDTO::getSno)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        // 学号 -> 学生
        Map<String, ClassStudent> snoToStudent = classStudentService.lambdaQuery()
                .in(ClassStudent::getSno, snos)
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
    private List<TeamScoreRankResp> calculateAndUpdateTeamScoresAndGetRankResp(
            Map<Long, List<XxtStudentScoreExcelDTO>> groupMap,
            Game game,
            Map<Long, Team> teamMap) {
        int maxCount = game.getTeamMemberCount();
        List<TeamScoreRankResp> resultList = new ArrayList<>();
        for (Map.Entry<Long, List<XxtStudentScoreExcelDTO>> entry : groupMap.entrySet()) {
            Long teamId = entry.getKey();
            List<XxtStudentScoreExcelDTO> teamScores = entry.getValue();
            // 计算本轮最高得分（取前 maxCount 个得分）
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
            // 取该组成绩的最晚提交时间
            LocalDateTime latestTime = teamScores.stream()
                    .map(XxtStudentScoreExcelDTO::getTime)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.MIN);
            Team team = teamMap.get(teamId);
            if (team != null) {
                // 累加本轮得分到历史总分
                int oldScore = Optional.ofNullable(team.getMemberScoreSum()).orElse(0);
                team.setMemberScoreSum(oldScore + thisRoundScore);
                // 更新数据库
                teamService.lambdaUpdate()
                        .eq(Team::getId, team.getId())
                        .eq(Team::getGameId, team.getGameId())
                        .update(team);
                // 构造返回对象
                TeamScoreRankResp resp = new TeamScoreRankResp();
                resp.setTeamId(teamId);
                resp.setTeamName(team.getLeaderName());
                resp.setThisRoundScore(thisRoundScore);
                resp.setSubmitTime(latestTime);
                resultList.add(resp);
            }
        }
        // 按本轮得分降序；得分相同时按提交时间升序排序
        resultList.sort(Comparator.comparingInt(TeamScoreRankResp::getThisRoundScore).reversed()
                .thenComparing(TeamScoreRankResp::getSubmitTime));
        return resultList;
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
                    if (rowIndex == 1) return; // 跳过表头
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

}




