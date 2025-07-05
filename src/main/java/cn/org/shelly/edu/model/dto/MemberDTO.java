package cn.org.shelly.edu.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberDTO {
    @Schema(description = "姓名")
    private String name;   // 姓名
    @Schema(description = "学号")
    private String id;     // 学号
}
