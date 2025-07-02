package cn.org.shelly.edu.service.impl;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.org.shelly.edu.constants.RedisConstants;
import cn.org.shelly.edu.service.PermissionService;
import cn.org.shelly.edu.service.RoleService;
import cn.org.shelly.edu.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        long id = StpUtil.getLoginIdAsLong();
        List<String> permissions = permissionService.getPermissionByUser(id);
        if(permissions.isEmpty()){
            return Collections.emptyList();
        }
        redisUtil.set(RedisConstants.USER_PERMISSION.getKey()+id,permissions);
        return permissions;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<String> roleNames =  roleService.getRoleNameByUser(Long.parseLong(loginId.toString()));
        if(CollUtil.isEmpty(roleNames)){
            return Collections.emptyList();
        }
        redisUtil.set(RedisConstants.USER_ROLE.getKey()+loginId,roleNames);
        return roleNames;
    }
}
