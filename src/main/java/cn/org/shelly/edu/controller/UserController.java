package cn.org.shelly.edu.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import cn.org.shelly.edu.annotation.AccessLimit;
import cn.org.shelly.edu.common.Result;
import cn.org.shelly.edu.constants.RedisConstants;
import cn.org.shelly.edu.enums.CodeEnum;
import cn.org.shelly.edu.model.pojo.User;
import cn.org.shelly.edu.model.req.LoginParam;
import cn.org.shelly.edu.model.req.PasswordReq;
import cn.org.shelly.edu.model.req.UserReq;
import cn.org.shelly.edu.model.resp.UserInfoResp;
import cn.org.shelly.edu.service.UserService;
import cn.org.shelly.edu.utils.EmailUtils;
import cn.org.shelly.edu.utils.PasswordUtils;
import cn.org.shelly.edu.utils.RedisUtil;
import cn.org.shelly.edu.utils.ValidateCodeUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name= "用户管理")
public class UserController {
    private final UserService userService;
    private final EmailUtils emailUtils;
    private final RedisUtil redisUtil;
    @Operation(summary = "登录")
    @SaIgnore
    @PostMapping("/login")
    public Result<UserInfoResp> login(@RequestBody LoginParam param){
        userService.login(param);
        return getUserInfo();
    }
    @Operation(summary = "退出登录")
    @SaIgnore
    @GetMapping("/logout")
    public Result<String> logout(){
        StpUtil.logout(StpUtil.getLoginId());
        return Result.success();
    }
    @Operation(summary = "发送验证码")
    @AccessLimit(seconds = 60, maxCount = 1)
    @SaIgnore
    @GetMapping("/sendCode")
    public Result<String> sendCode(@RequestParam("identifier") String identifier){
        String code = ValidateCodeUtils.generateValidateCodeUtils(6).toString();
        // 统一缓存 key
        String redisKey = RedisConstants.VERIFICATION_CODE.getKey() + identifier;
        EmailUtils.isValidEmail(identifier);
        // 调用邮箱服务发送验证码
        emailUtils.sendMailMessage(identifier, code);
        // 将生成的验证码存入Redis
        redisUtil.set(redisKey, code);
        redisUtil.expire(redisKey, 300);
        return Result.success();
    }



    @Operation(summary = "注册账号")
    @SaIgnore
    @PostMapping("/regist")
    public Result<String> regist(@RequestBody UserReq req){
        userService.regist(req);
        return Result.fail("注册成功！");
    }
    @Operation(summary = "获取用户信息（登录时自动获取）")
    @GetMapping("/info")
    public Result<UserInfoResp> getUserInfo(){
        String id = StpUtil.getLoginIdAsString();
        User user = redisUtil.get(RedisConstants.USER.getKey() + id,User.class);
        UserInfoResp info = new UserInfoResp()
                .setUsername(user.getUsername())
                .setNickname(user.getNickname())
                .setId(id)
                .setType(user.getType())
                .setAvatar(user.getAvatar());
        info.setToken(StpUtil.getTokenValue());
        // 存入缓存
        redisUtil.set(RedisConstants.USER_INFO,id,info);
        return Result.success(info);
    }
    @PutMapping("/pwd")
    @Operation(summary = "修改密码")
    public Result<?> updatePassword(@RequestBody PasswordReq param){
        User user = userService.getById(StpUtil.getLoginIdAsString());
        if(!PasswordUtils.match(param.oldPassword(),user.getPassword())){
            return Result.fail(CodeEnum.PASSWORD_ERROR);
        }
        if(!param.confirmPassword().equals(param.newPassword())){
            return  Result.fail(CodeEnum.PASSWORD_NOT_MATCH);
        }
        user.setPassword(PasswordUtils.encrypt(param.newPassword()));
        return Result.isSuccess(userService.updateById(user));
    }
    @PutMapping("/update")
    @Operation(summary = "更新个人信息(只能改头像，昵称)")
    public Result<?> update(@RequestBody UserReq param){
        userService.updateInfo(param);
        return Result.success();
    }
}
