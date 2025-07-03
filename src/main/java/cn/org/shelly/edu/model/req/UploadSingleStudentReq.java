package cn.org.shelly.edu.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UploadSingleStudentReq {

    @NotBlank(message = "学号不能为空")
    @Schema(description = "学号,不能为空")
    private String sno;

    @NotBlank(message = "姓名不能为空")
    @Schema(description = "姓名,不能为空")
    private String name;

    @NotNull(message = "班级ID不能为空")
    @Schema(description = "班级ID,不能为空")
    private Long cid;
    @Schema(description = "学生id，不一定都需要用")
    private Long id;
}