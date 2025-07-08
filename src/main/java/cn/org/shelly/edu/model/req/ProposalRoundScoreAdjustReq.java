package cn.org.shelly.edu.model.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProposalRoundScoreAdjustReq {
    @NotNull
    private Long gameId;

    @NotNull
    private Long teamId;

    @NotNull
    private Integer score;

    private String comment;
}
