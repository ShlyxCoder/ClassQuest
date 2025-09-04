package cn.org.shelly.edu.model.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProposalInitReq {

    @NotNull(message = "小组ID不能为空")
    private Long teamId;

    @NotNull(message = "初始积分不能为空")
    private BigDecimal initialScore;

    @NotNull
    private Long gameId;
}