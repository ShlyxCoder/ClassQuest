package cn.org.shelly.edu.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
@Data
@AllArgsConstructor
public class TeamInfoDTO {
    private Long teamNo;
    private String leader;
    private List<String> members;

}
