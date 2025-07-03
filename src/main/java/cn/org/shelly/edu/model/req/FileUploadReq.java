package cn.org.shelly.edu.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileUploadReq {
    @Schema(description = "文件")
    private MultipartFile file;
    private Long id;
}
