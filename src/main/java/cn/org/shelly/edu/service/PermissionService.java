package cn.org.shelly.edu.service;
import cn.org.shelly.edu.model.pojo.Permission;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author Shelly6
* @description 针对表【permission】的数据库操作Service
* @createDate 2025-07-02 10:22:21
*/
public interface PermissionService extends IService<Permission> {

    List<String> getPermissionByUser(long id);
}
