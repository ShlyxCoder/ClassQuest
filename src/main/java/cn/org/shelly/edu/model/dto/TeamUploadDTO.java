package cn.org.shelly.edu.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TeamUploadDTO {
    @Schema(description = "成功小组数")
    private Integer successTeamNum;
    @Schema(description = "成功学生数")
    private Integer successStudentNum;
}
