package cn.org.shelly.edu.service;
import cn.org.shelly.edu.model.pojo.User;
import cn.org.shelly.edu.model.req.LoginParam;
import cn.org.shelly.edu.model.req.UserReq;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Shelly6
* @description 针对表【user】的数据库操作Service
* @createDate 2025-07-02 10:22:21
*/
public interface UserService extends IService<User> {

    void login(LoginParam param);

    void regist(UserReq req);

    void updateInfo(UserReq param);
}
