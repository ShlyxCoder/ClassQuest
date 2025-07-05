package cn.org.shelly.edu.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamGroupResp {
    @Schema(description = "小组信息")
    private List<TeamDetailResp> teams;
    @Schema(description = "自由人信息")
    private List<FreeStudentResp> freeStudents;
}
