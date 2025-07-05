package cn.org.shelly.edu.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FreeStudentResp {
    @Schema(description = "学生ID")
    private Long studentId;
    @Schema(description = "学生姓名")
    private String studentName;
    @Schema(description = "学生学号")
    private String studentSno;

}