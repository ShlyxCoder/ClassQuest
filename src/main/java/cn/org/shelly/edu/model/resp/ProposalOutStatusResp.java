package cn.org.shelly.edu.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class ProposalOutStatusResp {
    @Schema(description = "小组ID")
    private Long teamId;
    @Schema(description = "小组得分")
    private Integer score;
    @Schema(description = "小组排名")
    private Integer rank;
    @Schema(description = "队长名")
    private String name;
    @Schema(description = "是否存活（0：out 1：alive）")
    private Boolean alive;
    @Schema(description = "淘汰时间")
    private Date outTime;
}
