package cn.org.shelly.edu.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ProposalOrderReq {
    @Schema(description = "游戏ID")
    private Long gameId;

    @Schema(description = "每轮小组列表，三轮", example = "[[1,2,3],[4,5,6],[7,8,9]]")
    private List<List<Long>> roundTeamIds;
}
