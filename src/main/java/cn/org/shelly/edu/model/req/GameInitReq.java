package cn.org.shelly.edu.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class GameInitReq {
    @Schema(description = "文件")
    private MultipartFile file;
    @Schema(description = "小组数量")
    private Integer teamNum;
    @Schema(description = "学生数量")
    private Integer studentNum;
    @Schema(description = "小组成员基准数量")
    private Integer teamMemberCount;
}
