package com.cdiom.backend.service;

import com.cdiom.backend.model.SysPermission;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 权限服务接口
 * 
 * @author cdiom
 */
public interface PermissionService {

    /**
     * 根据用户ID获取权限代码集合
     */
    Set<String> getPermissionCodesByUserId(Long userId);

    /**
     * 根据角色ID获取权限代码集合
     */
    Set<String> getPermissionCodesByRoleId(Long roleId);

    /**
     * 检查用户是否有指定权限
     */
    boolean hasPermission(Long userId, String permissionCode);

    /**
     * 检查用户是否有任意一个权限
     */
    boolean hasAnyPermission(Long userId, String... permissionCodes);

    /**
     * 获取所有权限列表
     */
    List<SysPermission> getAllPermissions();

    /**
     * 获取用户权限详情（区分角色权限和用户直接权限）
     * @param userId 用户ID
     * @return Map包含：rolePermissions（角色权限列表）、userPermissions（用户直接权限列表）、allPermissions（所有权限列表）
     */
    Map<String, Object> getUserPermissionDetails(Long userId);

    /**
     * 更新用户的直接权限
     * @param userId 用户ID
     * @param permissionIds 权限ID列表
     */
    void updateUserPermissions(Long userId, List<Long> permissionIds);
}

