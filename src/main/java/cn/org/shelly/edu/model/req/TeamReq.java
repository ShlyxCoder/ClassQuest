package cn.org.shelly.edu.model.req;

import lombok.Data;

@Data
public class TeamReq {
    private Long gameId;
    private Long leaderId;
    private String leaderName;
    private Integer totalMembers;

}
