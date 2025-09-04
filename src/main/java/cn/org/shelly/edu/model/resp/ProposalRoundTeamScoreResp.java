package cn.org.shelly.edu.model.resp;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProposalRoundTeamScoreResp {
    private Long teamId;
    private String leaderName;  // 小组组长姓名
    private BigDecimal score;      // 积分
    private Integer rank;       // 排名
}
