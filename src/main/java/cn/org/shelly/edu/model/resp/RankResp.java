package cn.org.shelly.edu.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "排行榜响应")
public class RankResp {

    @Schema(description = "小组排名")
    private List<TeamRankDTO> teamRanks;

    @Schema(description = "个人排名")
    private List<StudentRankDTO> studentRanks;

    @Data
    @Schema(description = "小组排名信息")
    public static class TeamRankDTO {
        @Schema(description = "小组编号")
        private Long teamId;

        @Schema(description = "组长姓名")
        private String leaderName;

        @Schema(description = "小组总得分")
        private Integer totalScore;
    }

    @Data
    @Schema(description = "个人排名信息")
    public static class StudentRankDTO {
        @Schema(description = "学生ID")
        private Long studentId;

        @Schema(description = "学生姓名")
        private String studentName;

        @Schema(description = "个人得分")
        private Integer individualScore;

        @Schema(description = "所属小组ID")
        private Long teamId;
    }
}
