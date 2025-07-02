package cn.org.shelly.edu.service.impl;
import cn.org.shelly.edu.constants.RedisConstants;
import cn.org.shelly.edu.mapper.PermissionMapper;
import cn.org.shelly.edu.model.pojo.Permission;
import cn.org.shelly.edu.service.PermissionService;
import cn.org.shelly.edu.utils.cache.Cache;
import cn.org.shelly.edu.utils.cache.CacheParam;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
* @author Shelly6
* @description 针对表【permission】的数据库操作Service实现
* @createDate 2025-07-02 10:22:21
*/
@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission>
    implements PermissionService {

    @Override
    @Cache(constants = RedisConstants.USER_PERMISSION)
    public List<String> getPermissionByUser(@CacheParam long id) {
        List<Permission> allPermissions = list();
        List<Permission> permissions = baseMapper.getPermissionByUser(id);
        return getSubPermissions(allPermissions, permissions).stream().map(Permission::getKeyName).toList();
    }
    public static Set<Permission> getSubPermissions(List<Permission> allPermissions, List<Permission> parentPermissions) {
        Set<Permission> subPermissions = new HashSet<>();
        for (Permission parentPermission : parentPermissions) {
            subPermissions.add(parentPermission);
            getSubPermissionRecursively(allPermissions, parentPermission.getId(), subPermissions);
        }
        return subPermissions;
    }

    /**
     * 递归获取子权限
     *
     * @param allPermissions 所有权限
     * @param parentId       父 ID
     * @param subPermissions 子权限
     */
    public static void getSubPermissionRecursively(List<Permission> allPermissions, Long parentId, Set<Permission> subPermissions) {
        for (Permission permission : allPermissions) {
            if (permission.getParentId().equals(parentId)) {
                subPermissions.add(permission);
                getSubPermissionRecursively(allPermissions, permission.getId(), subPermissions);
            }
        }
    }
}




