package cn.org.shelly.edu.model.resp;

import lombok.Data;

@Data
public class ProposalRoundTeamScoreResp {
    private Long teamId;
    private String leaderName;  // 小组组长姓名
    private Integer score;      // 积分
    private Integer rank;       // 排名
}
