package cn.org.shelly.edu.model.req;

import lombok.Data;

import java.util.List;

@Data
public class OutTeamReq {
    private Long gameId;
    private List<Long> teamIds;
}
