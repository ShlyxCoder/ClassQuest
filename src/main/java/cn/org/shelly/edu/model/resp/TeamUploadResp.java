package cn.org.shelly.edu.model.resp;

import lombok.Data;


@Data
public class TeamUploadResp {
    private Integer teamNum;
    private Integer studentNum;
    private Integer successTeamNum;
    private Integer failTeamNum;
    private Integer successStudentNum;
    private Integer failStudentNum;
    private Integer addStudentNum;
    private Long gameId;
}
