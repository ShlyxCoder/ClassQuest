package cn.org.shelly.edu.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class TeamUpdateReq {
    @NotNull
    @Schema(description = "小组id")
    private Long teamId;
    @Schema(description = "组长id")
    private Long leaderId;
    @Schema(description = "小组成员ids（含组长）")
    private List<Long> memberIds;       // 所有小组成员 studentId，必须包含 leaderId
}