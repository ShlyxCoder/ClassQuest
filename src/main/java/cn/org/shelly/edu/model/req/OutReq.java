package cn.org.shelly.edu.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class OutReq {
    private Long gameId;
    @Schema(description = "小组ID")
    private List<Long> teamIds;
    @Schema(description = "操作类型（2：淘汰 1：恢复）")
    private Integer type;
}
