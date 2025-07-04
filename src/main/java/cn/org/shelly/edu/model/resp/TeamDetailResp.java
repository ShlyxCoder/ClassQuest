package cn.org.shelly.edu.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class TeamDetailResp {
    @Schema(description = "小组id")
    private Long teamId;
    @Schema(description = "组长名称")
    private String leaderName;
    @Schema(description = "组长学生id")
    private Long leaderId;
    private Integer totalScore;
    @Schema(description = "小组人数")
    private Integer totalMembers;
    @Schema(description = "小组成员")
    private List<MemberDTO> members;

    @Data
    public static class MemberDTO {
        @Schema(description = "学生id")
        private Long studentId;
        @Schema(description = "学生名称")
        private String studentName;
        @Schema(description = "学生个人得分")
        private Integer individualScore;

        private Boolean isLeader;
    }
}
