package cn.org.shelly.edu.controller;

import cn.org.shelly.edu.common.Result;
import cn.org.shelly.edu.exception.CustomException;
import cn.org.shelly.edu.model.pojo.ClassStudent;
import cn.org.shelly.edu.model.pojo.Game;
import cn.org.shelly.edu.model.pojo.Team;
import cn.org.shelly.edu.model.pojo.TeamMember;
import cn.org.shelly.edu.model.req.TeamUpdateReq;
import cn.org.shelly.edu.model.resp.FreeStudentResp;
import cn.org.shelly.edu.model.resp.TeamDetailResp;
import cn.org.shelly.edu.model.resp.TeamGroupResp;
import cn.org.shelly.edu.model.resp.TeamUpdateResp;
import cn.org.shelly.edu.service.ClassStudentService;
import cn.org.shelly.edu.service.GameService;
import cn.org.shelly.edu.service.TeamMemberService;
import cn.org.shelly.edu.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/team")
@RequiredArgsConstructor
@Tag(name= "小组管理")
public class TeamController {

    private final TeamService teamService;

    private final TeamMemberService teamMemberService;

    private final ClassStudentService classStudentService;

    private final GameService gameService;
    @GetMapping("/template")
    @Operation(summary = "获取小组上传excel模板")
    public Result<Void> downloadTemplate(
            @Schema(description = "小组最大人数")
            @RequestParam(name = "memberCount", required = false, defaultValue = "3") int memberCount,
            HttpServletResponse response) {
        teamService.downloadTemplate(memberCount, response);
        return Result.success();
    }
    @GetMapping("/game/{id}")
    @Operation(summary = "获取小组分组信息（含自由人）")
    public Result<TeamGroupResp> getTeamsByGameId(@PathVariable("id") Long gameId) {
        // 查询所有 team
        List<Team> teams = teamService.lambdaQuery().eq(Team::getGameId, gameId).list();
        if (teams.isEmpty()) {
            return Result.success(new TeamGroupResp(Collections.emptyList(), Collections.emptyList()));
        }
        List<Long> teamIds = teams.stream().map(Team::getId).toList();
        // 查询所有成员
        List<TeamMember> allMembers = teamMemberService.lambdaQuery()
                .in(TeamMember::getTeamId, teamIds).list();
        // 分组成员
        Map<Long, List<TeamMember>> memberMap = allMembers.stream()
                .collect(Collectors.groupingBy(TeamMember::getTeamId));
        List<TeamDetailResp> teamDetailList = new ArrayList<>();
        for (Team team : teams) {
            TeamDetailResp dto = new TeamDetailResp();
            dto.setTeamId(team.getId());
            dto.setLeaderName(team.getLeaderName());
            dto.setLeaderId(team.getLeaderId());
            dto.setTotalScore(team.getTotalScore());
            dto.setTotalMembers(team.getTotalMembers());
            List<TeamDetailResp.MemberDTO> memberDTOs = memberMap.getOrDefault(team.getId(), Collections.emptyList())
                    .stream().map(m -> {
                        TeamDetailResp.MemberDTO md = new TeamDetailResp.MemberDTO();
                        md.setStudentId(m.getStudentId());
                        md.setStudentName(m.getStudentName());
                        md.setIndividualScore(m.getIndividualScore());
                        md.setIsLeader(m.getIsLeader() != null && m.getIsLeader() == 1);
                        return md;
                    }).toList();
            dto.setMembers(memberDTOs);
            teamDetailList.add(dto);
        }
        // 查询游戏对应的班级 ID
        Long cid = gameService.lambdaQuery()
                .eq(Game::getId, gameId)
                .select(Game::getCid)
                .oneOpt()
                .map(Game::getCid)
                .orElseThrow(() -> new CustomException("游戏不存在"));
        // 查询班级所有学生
        List<ClassStudent> allClassStudents = classStudentService.lambdaQuery()
                .eq(ClassStudent::getCid, cid)
                .select(ClassStudent::getId, ClassStudent::getName)
                .list();
        // 已加入小组的 studentId
        Set<Long> joinedStudentIds = allMembers.stream()
                .map(TeamMember::getStudentId)
                .collect(Collectors.toSet());
        // 自由人
        List<FreeStudentResp> freeStudents = allClassStudents.stream()
                .filter(s -> !joinedStudentIds.contains(s.getId()))
                .map(s -> new FreeStudentResp(s.getId(), s.getName()))
                .toList();
        return Result.success(new TeamGroupResp(teamDetailList, freeStudents));
    }


    @PutMapping("/game/{teamId}")
    @Operation(summary = "修改小组成员信息，并返回自由人")
    public Result<TeamUpdateResp> updateTeam(@PathVariable Long teamId, @RequestBody @Valid TeamUpdateReq req) {
        // 1. 查询旧小组
        Team team = teamService.getById(teamId);
        if (team == null) {
            throw new CustomException("小组不存在");
        }

        // 2. 查询该小组原始成员
        List<TeamMember> oldMembers = teamMemberService.lambdaQuery().eq(TeamMember::getTeamId, teamId).list();

        // 3. 删除旧成员
        if (!oldMembers.isEmpty()) {
            List<Long> memberIds = oldMembers.stream().map(TeamMember::getId).toList();
            teamMemberService.removeBatchByIds(memberIds);
        }
        // 4. 写入新成员
        List<ClassStudent> students = classStudentService.listByIds(req.getMemberIds());
        Map<Long, String> idToName = students.stream()
                .collect(Collectors.toMap(ClassStudent::getId, ClassStudent::getName));
        Date now = new Date();
        List<TeamMember> newMembers = req.getMemberIds().stream().map(id -> {
            TeamMember tm = new TeamMember();
            tm.setTeamId(teamId);
            tm.setStudentId(id);
            tm.setStudentName(idToName.get(id));
            tm.setIsLeader(req.getLeaderId().equals(id) ? 1 : 0);
            tm.setIndividualScore(0);
            return tm;
        }).toList();
        teamMemberService.saveBatch(newMembers);
        // 5. 更新 team 主表（组长 + 成员数量）
        team.setLeaderId(req.getLeaderId());
        team.setLeaderName(idToName.get(req.getLeaderId()));
        team.setTotalMembers(req.getMemberIds().size());
        team.setGmtUpdate(now);
        teamService.updateById(team);
        // 6. 查询当前班级的自由人
        Long cid = gameService.getById(team.getGameId()).getCid();
        List<ClassStudent> allStudents = classStudentService.lambdaQuery()
                .eq(ClassStudent::getCid, cid)
                .select(ClassStudent::getId, ClassStudent::getName)
                .list();
        Set<Long> assignedIds = teamMemberService.lambdaQuery()
                .select(TeamMember::getStudentId)
                .in(TeamMember::getTeamId, teamService.lambdaQuery()
                        .eq(Team::getGameId, team.getGameId()).select(Team::getId).list().stream().map(Team::getId).toList())
                .list().stream().map(TeamMember::getStudentId).collect(Collectors.toSet());
        List<FreeStudentResp> freeList = allStudents.stream()
                .filter(s -> !assignedIds.contains(s.getId()))
                .map(s -> new FreeStudentResp(s.getId(), s.getName()))
                .toList();

        // 7. 返回最新 team 详情
        TeamDetailResp resp = new TeamDetailResp();
        resp.setTeamId(team.getId());
        resp.setLeaderId(team.getLeaderId());
        resp.setLeaderName(team.getLeaderName());
        resp.setTotalMembers(team.getTotalMembers());
        resp.setTotalScore(team.getTotalScore());
        List<TeamDetailResp.MemberDTO> members = newMembers.stream().map(m -> {
            TeamDetailResp.MemberDTO dto = new TeamDetailResp.MemberDTO();
            dto.setStudentId(m.getStudentId());
            dto.setStudentName(m.getStudentName());
            dto.setIndividualScore(m.getIndividualScore());
            dto.setIsLeader(m.getIsLeader() != null && m.getIsLeader() == 1);
            return dto;
        }).toList();
        resp.setMembers(members);
        // 8. 统一封装响应
        TeamUpdateResp ret = new TeamUpdateResp();
        ret.setTeam(resp);
        ret.setFreeStudents(freeList);
        return Result.success(ret);
    }

}
