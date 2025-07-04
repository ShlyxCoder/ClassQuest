package cn.org.shelly.edu.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamInfoDTO {
    private Long teamNo;
    private MemberDTO leader;           // 组长，名字+学号
    private List<MemberDTO> members;    // 成员列表，每个成员名字+学号
}

