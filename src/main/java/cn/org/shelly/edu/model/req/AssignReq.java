package cn.org.shelly.edu.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;
@Data
public class AssignReq {
    @Schema(description = "游戏ID")
    private Long gameId;

    @Schema(description = "小组领地选择次数分配，key: 小组ID, value: 允许选择的格子数")
    private Map<Long, Integer> teamAssignCount;
}
