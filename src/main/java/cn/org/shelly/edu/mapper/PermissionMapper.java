package cn.org.shelly.edu.mapper;
import cn.org.shelly.edu.model.pojo.Permission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
* @author Shelly6
* @description 针对表【permission】的数据库操作Mapper
* @createDate 2025-07-02 10:22:21
* @Entity cn/org/shelly/edu/model/pojo.domain.Permission
*/
public interface PermissionMapper extends BaseMapper<Permission> {

    List<Permission> getPermissionByUser(long id);
}




