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
                .set(Classes::getCurrentStudents, studentMap.size())
                .update();
        return buildUploadResp(req, newStudents.size(), dto);
    }

    @Override
    public Boolean upload(MultipartFile file, Long gameId) {
        XxtStudentScoreListener listener = new XxtStudentScoreListener();
        try {
            EasyExcelFactory.read(file.getInputStream(),listener)
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
        //do-nothing
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
            if (members == null) {
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
                return null;
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
            MemberDTO leaderMember = dto.getLeader();
            if (leaderMember != null) {
                ClassStudent leader = map.get(leaderMember.getId());
                if (leader != null) {
                    memberList.add(createMember(team.getId(), leader, true));
                }
            }
            for (MemberDTO member : dto.getMembers()) {
                if (member != null) {
                    ClassStudent student = map.get(member.getId());
                    if (student != null) {
                        memberList.add(createMember(team.getId(), student, false));
                    } else {
                        log.warn("学生 {} 找不到，跳过", member.getName());
                    }
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
                .select(ClassStudent::getId, ClassStudent::getName, ClassStudent::getSno)
                .eq(ClassStudent::getCid, cid)
                .eq(ClassStudent::getIsDeleted,0)
                .list();
        return list.stream().collect(Collectors.toMap(ClassStudent::getSno, classStudent -> classStudent));
    }

}




