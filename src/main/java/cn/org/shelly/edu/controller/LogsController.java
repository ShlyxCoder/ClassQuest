package cn.org.shelly.edu.controller;

import cn.org.shelly.edu.common.PageInfo;
import cn.org.shelly.edu.common.Result;
import cn.org.shelly.edu.model.pojo.StudentScoreLog;
import cn.org.shelly.edu.model.pojo.TeamScoreLog;
import cn.org.shelly.edu.service.StudentScoreLogService;
import cn.org.shelly.edu.service.TeamScoreLogService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/log")
@RequiredArgsConstructor
@Tag(name= "日志管理")
public class LogsController {
    private final TeamScoreLogService teamScoreLogService;
    private final StudentScoreLogService studentScoreLogService;
    @GetMapping("/team/list")
    @Operation(summary = "查看小组得分日志列表")
    public Result<PageInfo<TeamScoreLog>> getTeamScoreLogs(
            @RequestParam(defaultValue = "1", required = false) Integer pageNum,
            @RequestParam(defaultValue = "10", required = false) Integer pageSize,
            @RequestParam Long gameId,
            @RequestParam Long teamId,
            @RequestParam(required = false) @Schema(description = "可选阶段：1=棋盘赛，2=提案赛") Integer phase
    ) {
        return Result.page(teamScoreLogService.lambdaQuery()
                .eq(TeamScoreLog::getGameId, gameId)
                .eq(TeamScoreLog::getTeamId, teamId)
                .eq(phase != null, TeamScoreLog::getPhase, phase)
                .orderByDesc(TeamScoreLog::getGmtCreate)
                .page(new Page<>(pageNum, pageSize))
        );
    }
    @GetMapping("/student/list")
    @Operation(summary = "查看学生得分日志列表")
    public Result<PageInfo<StudentScoreLog>> getStudentScoreLogs(
            @RequestParam(defaultValue = "1", required = false) Integer pageNum,
            @RequestParam(defaultValue = "10", required = false) Integer pageSize,
            @RequestParam Long gameId,
            @RequestParam Long studentId,
            @RequestParam(required = false) @Schema(description = "可选阶段：1=棋盘赛，2=提案赛") Integer phase
    ) {
        return Result.page(studentScoreLogService.lambdaQuery()
                .eq(StudentScoreLog::getGameId, gameId)
                .eq(StudentScoreLog::getStudentId, studentId)
                .eq(phase != null, StudentScoreLog::getPhase, phase)
                .orderByDesc(StudentScoreLog::getGmtCreate)
                .page(new Page<>(pageNum, pageSize))
        );
    }

}
