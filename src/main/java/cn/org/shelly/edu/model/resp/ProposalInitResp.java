package cn.org.shelly.edu.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ProposalInitResp {
    @Schema(description = "轮次")
    private Integer round;
    @Schema(description = "轮次提案次数")
    private List<Integer> roundTimes;
}
