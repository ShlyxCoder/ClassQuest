package cn.org.shelly.edu.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
@Data
@Accessors(chain = true)
public class UserInfoResp {
    @Schema(description = "用户id")
    private String id;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "昵称")
    private String nickname;
    @Schema(description = "用户头像")
    private String avatar;
    @Schema(description = "用户类型, 0：普通老师，-1：管理员老师 ")
    private Integer type;
    private String token;
}
