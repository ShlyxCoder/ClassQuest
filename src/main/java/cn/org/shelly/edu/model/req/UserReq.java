package cn.org.shelly.edu.model.req;

import cn.org.shelly.edu.model.pojo.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UserReq {
    private Long id;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "密码")
    private String password;
    @Schema(description = "昵称")
    private String nickname;
    @Schema(description = "头像")
    private String avatar;
    @Schema(description = "邮箱")
    private String email;
    @Schema(description = "用户类型, 0：普通老师，-1：管理员老师 ")
    private Integer type;
    private String code;

    public static User toUserPo(UserReq req) {
        return new User()
                .setUsername(req.getEmail())
                .setNickname(req.getNickname())
                .setAvatar(req.getAvatar())
                .setEmail(req.getEmail());
    }
}