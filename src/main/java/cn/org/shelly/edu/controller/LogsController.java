package cn.org.shelly.edu.controller;

import cn.org.shelly.edu.common.PageInfo;
import cn.org.shelly.edu.common.Result;
import cn.org.shelly.edu.model.pojo.StudentScoreLog;
import cn.org.shelly.edu.model.pojo.TeamMember;
import cn.org.shelly.edu.model.pojo.TeamScoreLog;
import cn.org.shelly.edu.service.StudentScoreLogService;
import cn.org.shelly.edu.service.TeamMemberService;
import cn.org.shelly.edu.service.TeamScoreLogService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
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
    private final TeamMemberService teamMemberService;
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

    @GetMapping("/stream")
    @Operation(summary = "老师助手对话接口")
    public Flux<String> chatStream(@RequestParam(required = false) String sno,
                                   @RequestParam(required = false) Long gameId,
                                   @RequestParam(required = false) String message,
                                   @RequestParam(defaultValue = "0")@Schema(description = "0=普通对话模式(默认)，1=学生画像分析模式") Integer type) {
        if (type == 0) {
            if (message == null || message.isBlank()) {
                return Flux.just("普通对话模式下，必须传入 message 参数。");
            }
            String prompt = "你是一位有经验、语气温和、逻辑清晰的老师，现在以老师身份回答老师提出的问题，请使用简洁清晰的语言作答。";
            return chatClient.prompt(new Prompt(List.of(
                    new SystemMessage(prompt),
                    new UserMessage(message)
            ))).stream().content();
        }

        if (type == 1) {
            if (StringUtils.isBlank(sno) || gameId == null) {
                return Flux.just("画像模式下，必须传入 studentId 和 gameId。");
            }
            Long studentId = teamMemberService.lambdaQuery()
                    .eq(TeamMember::getSno, sno)
                    .eq(TeamMember::getGameId, gameId)
                    .oneOpt()
                    .map(TeamMember::getStudentId)
                    .orElse(null);
            if (studentId == null) {
                return Flux.just("未找到该学生。");
            }
            List<StudentScoreLog> logs = studentScoreLogService.lambdaQuery()
                    .eq(StudentScoreLog::getStudentId, studentId)
                    .eq(StudentScoreLog::getGameId, gameId)
                    .orderByAsc(StudentScoreLog::getGmtCreate)
                    .list();
            if (logs.isEmpty()) {
                return Flux.just("未找到该学生在该游戏中的得分记录。");
            }
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("你是一名经验丰富、语气中肯温和的老师助理，请根据以下学生的得分记录和老师备注，生成一段50~100字的综合评价。记录既包括数字成绩，也包括老师的描述性点评：\n\n");
            promptBuilder.append("学生得分日志如下：\n");

            for (StudentScoreLog log : logs) {
                promptBuilder.append(formatLogForPrompt(log)).append("\n");
            }
            promptBuilder.append("\n请基于这些记录，生成自然语言评价。不使用条目、不输出代码。");
            return chatClient.prompt(new Prompt(List.of(
                    new SystemMessage(promptBuilder.toString()),
                    new UserMessage("请生成评价。")
            ))).stream().content();
        }
        return Flux.just("不支持的 type 类型。请传入 0（普通对话）或 1（学生画像）");
    }
    private String formatLogForPrompt(StudentScoreLog log) {
        String round = log.getRound() != null ? "第" + log.getRound() + "轮" : "未知轮次";
        String phase = switch (log.getPhase()) {
            case 1 -> "棋盘赛";
            case 2 -> "提案赛";
            default -> "未知阶段";
        };
        String reason = switch (log.getReason()) {
            case 1 -> "老师加分";
            case 2 -> "老师扣分";
            case 3 -> "学习通导入成绩";
            case 4 -> "描述性记录";
            default -> "其他操作";
        };
        StringBuilder sb = new StringBuilder();
        sb.append("- ").append(round).append(" ").append(phase).append("：")
                .append(reason).append("，得分 ").append(log.getScore());
        if (log.getComment() != null && !log.getComment().isBlank()) {
            sb.append("，备注：").append(log.getComment());
        }
        return sb.toString();
    }

}
