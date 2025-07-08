package cn.org.shelly.edu.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class DebateEvaluationReq {
    private Long gameId;
    @Schema(description = "提案ID")
    private Long proposalId;
    @Schema(description = "老师正方给分")
    private Integer teacherScorePro;
    @Schema(description = "老师反方给分")
    private Integer teacherScoreCon;

    private List<StudentGroupScore> studentScores;

    @Data
    public static class StudentGroupScore {
        @Schema(description = "打分队伍ID")
        private Long fromTeamId;
        @Schema(description = "正方打分")
        private Integer scorePro;
        @Schema(description = "反方打分")
        private Integer scoreCon;
    }
}
