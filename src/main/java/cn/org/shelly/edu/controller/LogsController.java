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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/log")
@RequiredArgsConstructor
@Tag(name= "日志管理")
public class LogsController {
    private final TeamScoreLogService teamScoreLogService;
    private final StudentScoreLogService studentScoreLogService;
    private final ChatClient chatClient;
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

    @GetMapping(value = "/stream")
    @Operation(summary = "流式对话")
    public Flux<String> chatStream(@RequestParam String message) {
        String prompt = "你是一名经验丰富、语气中肯又不失亲切感的老师助手，现在你将根据学生的得分日志，对学生进行简要分析与评价。请注意：\n" +
                "\n" +
                "1. **角色定位**：你是一名老师，语言风格需要具备教师特有的温和、理性与指导性，避免冷冰冰或刻意夸张。\n" +
                "2. **分析维度**包括：\n" +
                "   - 本轮成绩（是否进步、是否退步）\n" +
                "   - 与前几轮成绩的对比（有无明显趋势）\n" +
                "   - 在组内或全体中的相对排名（如提供了）\n" +
                "   - 总体表现是否稳定\n" +
                "   - 是否存在爆发/掉队等异常情况\n" +
                "3. **评价风格**需中肯有逻辑，避免无根据夸赞或贬低；可以适度鼓励、提出建议。\n" +
                "4. **输出格式**：\n" +
                "   - 一段自然语言的文字评价，长度约50~100字；\n" +
                "   - 不要输出代码、表格或条目，直接输出完整评价语句即可。\n" +
                "\n" +
                "下面是一个学生的得分日志数据：\n" +
                "\n" +
                "{{ 填入学生得分数据（JSON 或结构化文本） }}\n" +
                "\n" +
                "请根据上述要求，生成一段中肯、带教师口吻的文字评价。\n";
        return chatClient.prompt(new Prompt(List.of( new SystemMessage(prompt),new UserMessage(message))))
                .stream()
                .content();
    }
}
