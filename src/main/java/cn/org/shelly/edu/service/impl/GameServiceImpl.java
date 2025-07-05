package cn.org.shelly.edu.service.impl;

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
import cn.org.shelly.edu.model.req.GameInitReq;
import cn.org.shelly.edu.model.resp.TeamUploadResp;
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
    public Boolean upload(MultipartFile file, Long gameId) {
        Game game = getById(gameId);
        if (game == null) {
            throw new CustomException("游戏不存在");
        }
        if(game.getStatus() != 1){
            log.warn("游戏已结束");
        }
        if (game.getStage() != 1 || game.getChessPhase() != 0) {
            throw new CustomException("当前不可进行该操作");
        }
        XxtStudentScoreListener listener = new XxtStudentScoreListener();
        try {
            EasyExcelFactory.read(file.getInputStream(), listener)
                    .head(XxtStudentScoreExcelDTO.class)
                    .sheet()
                    .headRowNumber(6)
                    .doRead();
        } catch (ExcelReadStopException e) {
            log.info("导入数据结束，读取中断：{}", e.getMessage());
        } catch (IOException e) {
            throw new CustomException(CodeEnum.SYSTEM_ERROR);
        }
        List<XxtStudentScoreExcelDTO> scores = listener.getData();
        if (CollectionUtils.isEmpty(scores)) {
            log.warn("上传数据为空");
            throw new CustomException("上传数据为空");
        }
        // 获取上传的学号集合
        Set<String> snos = scores.stream()
                .map(XxtStudentScoreExcelDTO::getSno)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        // 查询学生信息
        List<ClassStudent> students = classStudentService.lambdaQuery()
                .in(ClassStudent::getSno, snos)
                .list();
        // 获取学号-学生map
        Map<String, ClassStudent> snoToStudentMap = students.stream()
                .collect(Collectors.toMap(ClassStudent::getSno, s -> s));
        // 查询当前 game 下的 team 列表
        List<Team> teams = teamService.lambdaQuery()
                .eq(Team::getGameId, gameId)
                .list();
        // teamId → team
        Map<Long, Team> teamMap = teams.stream()
                .collect(Collectors.toMap(Team::getId, t -> t));
        // 查询 teamMember
        List<Long> teamIds = new ArrayList<>(teamMap.keySet());
        List<TeamMember> teamMembers = teamMemberService.lambdaQuery()
                .in(TeamMember::getTeamId, teamIds)
                .list();
        // studentId → teamId 映射
        Map<Long, Long> studentIdToTeamId = teamMembers.stream()
                .collect(Collectors.toMap(TeamMember::getStudentId, TeamMember::getTeamId));
        // 按 teamId 分组成员成绩
        Map<Long, List<XxtStudentScoreExcelDTO>> teamGrouped = new HashMap<>();
        for (XxtStudentScoreExcelDTO dto : scores) {
            // 合法的数据
            boolean valid = true;
            ClassStudent student = snoToStudentMap.get(dto.getSno());
            if (student == null) {
                log.warn("学号 {} 未在班级中找到，跳过", dto.getSno());
                valid = false;
            }
            // 获取 teamId
            Long teamId = valid ? studentIdToTeamId.get(student.getId()) : null;
            if (valid && teamId == null) {
                log.warn("学生 {} 未分组，跳过", student.getName());
                valid = false;
            }
            if (!valid) {
                continue;
            }
            teamGrouped.computeIfAbsent(teamId, k -> new ArrayList<>()).add(dto);
        }
        // 获取 game 的积分计算限制

        int maxCount = game.getTeamMemberCount();
        // 更新每个小组的总分
        for (Map.Entry<Long, List<XxtStudentScoreExcelDTO>> entry : teamGrouped.entrySet()) {
            Long teamId = entry.getKey();
            List<XxtStudentScoreExcelDTO> teamScores = entry.getValue();
            List<Integer> scoreList = teamScores.stream()
                    .map(dto -> {
                        try {
                            return Integer.parseInt(dto.getScore());
                        } catch (NumberFormatException e) {
                            log.warn("学号 {} 分数非法 [{}]，按 0 处理", dto.getSno(), dto.getScore());
                            return 0;
                        }
                    })
                    .sorted(Comparator.reverseOrder())
                    .limit(maxCount)
                    .toList();
            int totalScore = scoreList.stream().mapToInt(Integer::intValue).sum();
            Team team = teamMap.get(teamId);
            if (team != null) {
                team.setMemberScoreSum(totalScore);
                boolean updated = teamService.lambdaUpdate()
                        .eq(Team::getId, team.getId())
                        .eq(Team::getGameId, team.getGameId())
                        .update(team);
                if (updated) {
                    log.info("更新小组 {} 总积分为 {}", teamId, totalScore);
                } else {
                    log.error("更新小组 {} 积分失败", teamId);
                }
            }
        }
        //TODO 小组日志, 小组成员日志
        game.setChessPhase(1);
        game.setLastSavedAt(new Date());
        updateById(game);
        return true;
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




