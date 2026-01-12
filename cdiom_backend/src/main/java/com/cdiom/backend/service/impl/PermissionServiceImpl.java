package com.cdiom.backend.service.impl;

import com.cdiom.backend.mapper.SysPermissionMapper;
import com.cdiom.backend.mapper.SysUserMapper;
import com.cdiom.backend.model.SysUser;
import com.cdiom.backend.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 权限服务实现类
 * 
 * @author cdiom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final SysPermissionMapper permissionMapper;
    private final SysUserMapper userMapper;

    @Override
    public Set<String> getPermissionCodesByUserId(Long userId) {
        try {
            if (userId == null) {
                log.warn("用户ID为空，返回空权限集合");
                return new HashSet<>();
            }
            
            SysUser user = userMapper.selectById(userId);
            if (user == null) {
                log.warn("用户不存在，userId: {}", userId);
                return new HashSet<>();
            }
            
            if (user.getRoleId() == null) {
                log.warn("用户角色ID为空，userId: {}", userId);
                return new HashSet<>();
            }
            
            return getPermissionCodesByRoleId(user.getRoleId());
        } catch (Exception e) {
            log.error("获取用户权限失败，userId: {}", userId, e);
            return new HashSet<>();
        }
    }

    @Override
    public Set<String> getPermissionCodesByRoleId(Long roleId) {
        try {
            if (roleId == null) {
                log.warn("角色ID为空，返回空权限集合");
                return new HashSet<>();
            }
            
            // 系统管理员拥有所有权限
            if (roleId == 1) {
                return new HashSet<>(List.of("*")); // 通配符表示所有权限
            }
            
            List<String> permissionCodes = permissionMapper.selectPermissionCodesByRoleId(roleId);
            if (CollectionUtils.isEmpty(permissionCodes)) {
                log.debug("角色 {} 没有配置权限，返回空权限集合", roleId);
                return new HashSet<>();
            }
            
            return new HashSet<>(permissionCodes);
        } catch (Exception e) {
            log.error("获取角色权限失败，roleId: {}", roleId, e);
            return new HashSet<>();
        }
    }

    @Override
    public boolean hasPermission(Long userId, String permissionCode) {
        try {
            if (userId == null || permissionCode == null || permissionCode.trim().isEmpty()) {
                return false;
            }
            
            Set<String> userPermissions = getPermissionCodesByUserId(userId);
            
            // 如果用户拥有通配符权限，则拥有所有权限
            if (userPermissions.contains("*")) {
                return true;
            }
            
            return userPermissions.contains(permissionCode);
        } catch (Exception e) {
            log.error("检查用户权限失败，userId: {}, permissionCode: {}", userId, permissionCode, e);
            return false;
        }
    }

    @Override
    public boolean hasAnyPermission(Long userId, String... permissionCodes) {
        try {
            if (userId == null || permissionCodes == null || permissionCodes.length == 0) {
                return false;
            }
            
            Set<String> userPermissions = getPermissionCodesByUserId(userId);
            
            // 如果用户拥有通配符权限，则拥有所有权限
            if (userPermissions.contains("*")) {
                return true;
            }
            
            for (String permissionCode : permissionCodes) {
                if (permissionCode != null && userPermissions.contains(permissionCode)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("检查用户权限失败，userId: {}, permissionCodes: {}", userId, List.of(permissionCodes), e);
            return false;
        }
    }
}


