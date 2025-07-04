package cn.org.shelly.edu.model.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamGroupResp {
    private List<TeamDetailResp> teams;
    private List<FreeStudentResp> freeStudents;
}
