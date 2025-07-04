package cn.org.shelly.edu.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class LoginParam {
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "密码")
    private String password;
    @Schema(description = "邮箱验证码")
    private String emailCode;
//    @Schema(description = "图形验证码")
//    private String captchaCode;
    @Schema(description = "登录类型 0:邮箱 1:账密(默认)")
    private int loginType = 1;

}