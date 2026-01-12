package com.cdiom.backend.service;

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
}

