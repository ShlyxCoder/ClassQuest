package cn.org.shelly.edu.model.resp;

import lombok.Data;

import java.util.List;

@Data
public class TeamUpdateResp {
    private TeamDetailResp team;
    private List<FreeStudentResp> freeStudents;
}
