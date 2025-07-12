package cn.org.shelly.edu.service.impl;
import cn.dev33.satoken.stp.StpUtil;
import cn.org.shelly.edu.constants.RedisConstants;
import cn.org.shelly.edu.enums.CodeEnum;
import cn.org.shelly.edu.exception.CustomException;
import cn.org.shelly.edu.mapper.UserMapper;
import cn.org.shelly.edu.model.pojo.User;
import cn.org.shelly.edu.model.req.LoginParam;
import cn.org.shelly.edu.model.req.UserReq;
import cn.org.shelly.edu.service.UserService;
import cn.org.shelly.edu.utils.EmailUtils;
import cn.org.shelly.edu.utils.PasswordUtils;
import cn.org.shelly.edu.utils.RedisUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static cn.org.shelly.edu.constants.RedisConstants.CAPTCHA_CODE_KEY_PREFIX;
import static cn.org.shelly.edu.utils.EmailUtils.isValidEmail;

/**
* @author Shelly6
* @description 针对表【user】的数据库操作Service实现
* @createDate 2025-07-02 10:22:21
*/
@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {
    private final RedisUtil redisUtil;

    @Override
    public void login(LoginParam param) {
//        int loginType = param.getLoginType();
//        String identity = loginType == 0 ? param.getEmail() : param.getUsername();
//        String failKey = RedisConstants.LOGIN_FAIL_KEY_PREFIX +  identity;
//        String captchaKey = CAPTCHA_CODE_KEY_PREFIX + identity;
//        int failCount = Optional.ofNullable(redisUtil.get(failKey, Integer.class)).orElse(0);
//        // 登录失败次数达到阈值，校验图形验证码
//        if (failCount >= 3) {
//            String expectedCaptcha = redisUtil.get(captchaKey, String.class);
//            if (StringUtils.isBlank(param.getCaptchaCode()) || !param.getCaptchaCode().equalsIgnoreCase(expectedCaptcha)) {
//                throw new CustomException("图形验证码错误或已过期");
//            }
//        }
        User user;
        if(param.getLoginType() == 0){
            isValidEmail(param.getUsername());
            checkVerificationCode(param.getUsername(),param.getUsername());
            user = lambdaQuery()
                    .eq(User::getEmail,param.getUsername())
                    .one();
            if(user == null){
                throw new CustomException(CodeEnum.USER_NOT_FOUND);
            }
        }
        else{
            String username = param.getUsername();
            String password = param.getPassword();
            user = lambdaQuery()
                    .eq(User::getUsername,username)
                    .one();
            if(user == null){
                throw new CustomException(CodeEnum.USER_NOT_FOUND);
            }
            if(!PasswordUtils.match(password,user.getPassword())){
                throw new CustomException(CodeEnum.PASSWORD_ERROR);
            }
        }
        //redisUtil.remove(failKey);
        handleLoginSuccess(user);
    }

    @Override
    public void regist(UserReq req) {
        log.info("用户注册请求：{}", req);
        // 校验邮箱
        String email = StringUtils.trimToEmpty(req.getEmail());
        if (StringUtils.isBlank(email)) {
            throw new CustomException("邮箱不能为空");
        }
        // 校验验证码
        String code = StringUtils.trimToEmpty(req.getCode());
        if (StringUtils.isBlank(code)) {
            throw new CustomException("验证码不能为空");
        }
        // 校验密码
        String password = req.getPassword();
        if (StringUtils.isBlank(password)) {
            throw new CustomException("密码不能为空");
        }
        // 校验邮箱格式
        EmailUtils.isValidEmail(email);
        // 设置用户名为邮箱
        req.setUsername(email);
        req.setEmail(email);
        // 读取Redis验证码
        String redisKey = RedisConstants.VERIFICATION_CODE.getKey() + email;
        Object redisValObj = redisUtil.getObject(redisKey);
        String redisCode = redisValObj != null ? redisValObj.toString().trim() : null;
        if (redisCode == null || redisUtil.getTime(redisKey) == 0) {
            throw new CustomException(CodeEnum.CODE_EXPIRED);
        }
        log.info("输入验证码: [{}], Redis验证码: [{}]", code, redisCode);
        if (!code.equals(redisCode)) {
            throw new CustomException(CodeEnum.CODE_ERROR);
        }
        // 校验邮箱是否已注册
        long count = lambdaQuery().eq(User::getEmail, email).count();
        if (count > 0) {
            throw new CustomException("邮箱已存在");
        }
        // 构建用户对象并保存
        User user = UserReq.toUserPo(req);
        user.setType(0);
        user.setPassword(PasswordUtils.encrypt(password));
        save(user);
    }


    @Override
    public void updateInfo(UserReq param) {
        User old = lambdaQuery()
                .eq(User::getId, param.getId())
                .one();
        if(old == null){
            throw new CustomException("用户不存在");
        }
        User po = UserReq.toUserPo(param);
        log.info("更新用户信息：{}", po);
        po.setUsername(old.getUsername());
        po.setPassword(old.getPassword());
        po.setEmail(old.getEmail());
        updateById(po);
    }

    private void handleLoginSuccess(User user) {
        StpUtil.login(user.getId());
        redisUtil.set(RedisConstants.USER_TOKEN, String.valueOf(user.getId()), StpUtil.getTokenValue());
        redisUtil.set(RedisConstants.USER, String.valueOf(user.getId()), JSON.toJSONString(user));
    }
    public void checkVerificationCode(String s, String code) {
        if (StringUtils.isBlank(code)) {
            throw new CustomException(CodeEnum.CODE_ERROR);
        }
        String redisKey = RedisConstants.VERIFICATION_CODE.getKey() + s;
        if (redisUtil.getObject(redisKey) == null || redisUtil.getTime(redisKey) == 0) {
            throw new CustomException(CodeEnum.CODE_EXPIRED);
        }
        // 验证验证码是否匹配
        if (!code.equals(redisUtil.getObject(redisKey).toString())) {
            throw new CustomException(CodeEnum.CODE_ERROR);
        }
    }
}




