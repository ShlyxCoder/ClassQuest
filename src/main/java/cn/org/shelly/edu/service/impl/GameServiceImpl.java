package cn.org.shelly.edu.service.impl;

import cn.org.shelly.edu.exception.CustomException;
import cn.org.shelly.edu.mapper.GameMapper;
import cn.org.shelly.edu.model.dto.TeamInfoDTO;
import cn.org.shelly.edu.model.dto.TeamUploadDTO;
import cn.org.shelly.edu.model.pojo.*;
import cn.org.shelly.edu.model.req.GameInitReq;
import cn.org.shelly.edu.model.resp.TeamUploadResp;
import cn.org.shelly.edu.service.*;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
        List<TeamInfoDTO> groupList = parseExcel(req.getFile());
        Game game = createGame(req);
        Map<String, ClassStudent> studentMap = getClassStudentMap(req.getCid());
        List<ClassStudent> newStudents = collectMissingStudents(groupList, studentMap, req.getCid());
        if (!newStudents.isEmpty()) {
            classStudentService.saveBatch(newStudents);
            for (ClassStudent s : newStudents) {
                studentMap.put(s.getName(), s);
            }
        }
        TeamUploadDTO dto = saveTeamsAndMembers(groupList, game, studentMap);
        classService.lambdaUpdate()
                .eq(Classes::getId, req.getCid())
                .set(Classes::getCurrentStudents, studentMap.size())
                .update();
        return buildUploadResp(req, newStudents.size(), dto);
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

        // 解析组长
        String leader = row.getOrDefault(1, "").trim();
        if (leader.isEmpty()) {
            log.warn("第 {} 行跳过：组长为空", rowIndex);
            return;
        }
        // 解析小组号
        String teamNoStr = row.getOrDefault(0, "").trim();
        Long teamNo = null;
        if (!teamNoStr.isEmpty()) {
            try {
                teamNo = Long.parseLong(teamNoStr);
            } catch (NumberFormatException e) {
                log.warn("第 {} 行小组号格式非法 [{}]，将自动分配", rowIndex, teamNoStr);
            }
        }
        // 自动分配不重复小组号
        if (teamNo == null || existingTeamNos.contains(teamNo)) {
            while (existingTeamNos.contains(teamSeq.get())) {
                teamSeq.incrementAndGet();
            }
            teamNo = teamSeq.getAndIncrement();
        }

        existingTeamNos.add(teamNo);

        // 解析成员
        List<String> members = new ArrayList<>();
        for (int i = 2; i < row.size(); i++) {
            String member = row.get(i);
            if (member != null && !member.trim().isEmpty()) {
                members.add(member.trim());
            }
        }

        groupList.add(new TeamInfoDTO(teamNo, leader, members));
    }

    private Game createGame(GameInitReq req) {
        Game game = new Game()
                .setCid(req.getCid())
                .setStudentCount(req.getStudentNum())
                .setTeamCount(req.getTeamNum())
                .setTeamMemberCount(req.getTeamMemberCount())
                .setStage(1)
                .setChessRound(0)
                .setChessPhase(0)
                .setLastSavedAt(new Date())
                .setStatus(1);
        save(game);
        return game;
    }
    private List<ClassStudent> collectMissingStudents(List<TeamInfoDTO> groupList,
                                                      Map<String, ClassStudent> map,
                                                      Long cid) {
        List<ClassStudent> list = new ArrayList<>();
        Set<String> exists = new HashSet<>(map.keySet());
        for (TeamInfoDTO dto : groupList) {
            if (!exists.contains(dto.getLeader())) {
                list.add(newStudent(dto.getLeader(), cid));
            }
            for (String member : dto.getMembers()) {
                if (!exists.contains(member)) {
                    list.add(newStudent(member, cid));
                }
            }
        }
        return list;
    }

    private ClassStudent newStudent(String name, Long cid) {
        ClassStudent student = new ClassStudent();
        student.setCid(cid);
        student.setName(name);
        student.setIsDeleted(0);
        return student;
    }
    private TeamUploadDTO saveTeamsAndMembers(List<TeamInfoDTO> groupList,
                                              Game game,
                                              Map<String, ClassStudent> map) {
        List<Team> teamList = new ArrayList<>();
        List<TeamMember> memberList = new ArrayList<>();
        for (TeamInfoDTO dto : groupList) {
            ClassStudent leader = map.get(dto.getLeader());
            if (leader == null) continue;
            Team team = new Team();
            team.setGameId(game.getId());
            team.setLeaderId(leader.getId());
            team.setLeaderName(leader.getName());
            team.setTotalMembers(1 + dto.getMembers().size());
            team.setTotalScore(0);
            team.setAlive(1);
            teamList.add(team);
        }
        teamService.saveBatch(teamList);
        for (int i = 0; i < teamList.size(); i++) {
            Team team = teamList.get(i);
            TeamInfoDTO dto = groupList.get(i);
            // 组长
            ClassStudent leader = map.get(dto.getLeader());
            if (leader != null) {
                memberList.add(createMember(team.getId(), leader, true));
            }
            // 成员
            for (String name : dto.getMembers()) {
                ClassStudent student = map.get(name);
                if (student != null) {
                    memberList.add(createMember(team.getId(), student, false));
                } else {
                    log.warn("学生 {} 找不到，跳过", name);
                }
            }
        }
        teamMemberService.saveBatch(memberList);
        return new TeamUploadDTO(teamList.size(), memberList.size());
    }

    private TeamMember createMember(Long teamId, ClassStudent student, boolean isLeader) {
        TeamMember m = new TeamMember();
        m.setTeamId(teamId);
        m.setStudentId(student.getId());
        m.setStudentName(student.getName());
        m.setIsLeader(isLeader ? 1 : 0);
        m.setIndividualScore(0);
        return m;
    }
    private TeamUploadResp buildUploadResp(GameInitReq req, int size,TeamUploadDTO dto) {
        TeamUploadResp resp = new TeamUploadResp();
        resp.setTeamNum(req.getTeamNum());
        resp.setStudentNum(req.getStudentNum());
        resp.setSuccessTeamNum(dto.getSuccessTeamNum());
        resp.setFailTeamNum(req.getTeamNum() - dto.getSuccessTeamNum());
        resp.setSuccessStudentNum(dto.getSuccessStudentNum());
        resp.setFailStudentNum(req.getStudentNum() - dto.getSuccessStudentNum());
        resp.setAddStudentNum(size);
        return resp;
    }
    private Map<String, ClassStudent> getClassStudentMap(Long cid) {
        List<ClassStudent> list = classStudentService
                .lambdaQuery()
                .select(ClassStudent::getId, ClassStudent::getName)
                .eq(ClassStudent::getCid, cid)
                .eq(ClassStudent::getIsDeleted,0)
                .list();
        return list.stream().collect(Collectors.toMap(ClassStudent::getName, classStudent -> classStudent));
    }

}




