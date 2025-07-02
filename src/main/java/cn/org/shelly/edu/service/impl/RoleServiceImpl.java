package cn.org.shelly.edu.service.impl;
import cn.hutool.core.collection.CollUtil;
import cn.org.shelly.edu.constants.RedisConstants;
import cn.org.shelly.edu.mapper.RoleMapper;
import cn.org.shelly.edu.model.pojo.Role;
import cn.org.shelly.edu.model.pojo.UserRole;
import cn.org.shelly.edu.service.RoleService;
import cn.org.shelly.edu.service.UserRoleService;
import cn.org.shelly.edu.utils.cache.Cache;
import cn.org.shelly.edu.utils.cache.CacheParam;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
* @author Shelly6
* @description 针对表【role】的数据库操作Service实现
* @createDate 2025-07-02 10:22:21
*/
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role>
    implements RoleService {
    @Autowired
    private UserRoleService userRoleService;
    @Override
    @Cache(constants = RedisConstants.USER_ROLE)
    public List<String> getRoleNameByUser(@CacheParam Long id) {
        return getRoleByUser(id).stream().map(Role::getRoleName).toList();
    }
    @Override
    public List<Role> getRoleByUser(Long id) {
        List<Long> roleIdList = userRoleService.lambdaQuery().select(UserRole::getRoleId)
                .eq(UserRole::getUserId, id).list()
                .stream().map(UserRole::getRoleId).toList();
        if(CollUtil.isEmpty(roleIdList)){
            return Collections.emptyList();
        }
        return lambdaQuery().in(Role::getId,roleIdList).list();
    }
}




