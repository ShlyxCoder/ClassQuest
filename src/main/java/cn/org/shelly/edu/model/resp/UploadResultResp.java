package cn.org.shelly.edu.model.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadResultResp {
    private int successCount;
    private int failCount;
}
