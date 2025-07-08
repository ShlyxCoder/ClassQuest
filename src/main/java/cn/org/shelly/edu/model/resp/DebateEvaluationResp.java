package cn.org.shelly.edu.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class DebateEvaluationResp {

    @Schema(description = "正方得分")
    private Double proFinalScore;
    @Schema(description = "反方得分")
    private Double conFinalScore;
    @Schema(description = "获胜小组ids")
    private List<Integer> ids;
}
