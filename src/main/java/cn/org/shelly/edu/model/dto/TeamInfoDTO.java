package cn.org.shelly.edu.model.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamInfoDTO {
    @Schema(description = "小组编号")
    private Long teamNo;
    @Schema(description = "小组名称")
    private MemberDTO leader;           // 组长，名字+学号
    @Schema(description = "小组成员")
    private List<MemberDTO> members;    // 成员列表，每个成员名字+学号
}

