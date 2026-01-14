package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cdiom.backend.mapper.SysPermissionMapper;
import com.cdiom.backend.mapper.SysUserMapper;
import com.cdiom.backend.mapper.SysUserPermissionMapper;
import com.cdiom.backend.model.SysPermission;
import com.cdiom.backend.model.SysUser;
import com.cdiom.backend.model.SysUserPermission;
import com.cdiom.backend.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

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
    private final SysUserPermissionMapper userPermissionMapper;

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
            
            // 超级管理员（角色ID=6）拥有所有权限
            if (user.getRoleId() != null && user.getRoleId() == 6) {
                return new HashSet<>(List.of("*")); // 通配符表示所有权限
            }
            
            // 查询用户的所有权限（角色权限 + 用户直接权限）
            List<String> permissionCodes = permissionMapper.selectAllPermissionCodesByUserId(userId);
            if (CollectionUtils.isEmpty(permissionCodes)) {
                log.debug("用户 {} 没有配置权限，返回空权限集合", userId);
                return new HashSet<>();
            }
            
            return new HashSet<>(permissionCodes);
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
            
            // 超级管理员（角色ID=6）拥有所有权限
            if (roleId == 6) {
                return new HashSet<>(List.of("*")); // 通配符表示所有权限
            }
            
            // 系统管理员只拥有系统功能权限，不再拥有所有权限
            // 从数据库中查询系统管理员的权限配置
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

    @Override
    public List<SysPermission> getAllPermissions() {
        try {
            LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysPermission::getPermissionType, 3); // 只查询接口权限
            wrapper.orderByAsc(SysPermission::getSortOrder);
            wrapper.orderByAsc(SysPermission::getPermissionCode);
            return permissionMapper.selectList(wrapper);
        } catch (Exception e) {
            log.error("获取所有权限列表失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, Object> getUserPermissionDetails(Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (userId == null) {
                log.warn("用户ID为空");
                result.put("rolePermissions", new ArrayList<>());
                result.put("userPermissions", new ArrayList<>());
                result.put("allPermissions", new ArrayList<>());
                return result;
            }

            SysUser user = userMapper.selectById(userId);
            if (user == null) {
                log.warn("用户不存在，userId: {}", userId);
                result.put("rolePermissions", new ArrayList<>());
                result.put("userPermissions", new ArrayList<>());
                result.put("allPermissions", new ArrayList<>());
                return result;
            }

            // 超级管理员特殊处理
            if (user.getRoleId() != null && user.getRoleId() == 6) {
                List<SysPermission> allPermissions = getAllPermissions();
                result.put("rolePermissions", allPermissions);
                result.put("userPermissions", new ArrayList<>());
                result.put("allPermissions", allPermissions);
                return result;
            }

            // 获取角色权限
            List<String> rolePermissionCodes = permissionMapper.selectPermissionCodesByRoleId(user.getRoleId());
            List<SysPermission> rolePermissions = new ArrayList<>();
            if (!CollectionUtils.isEmpty(rolePermissionCodes)) {
                LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
                wrapper.in(SysPermission::getPermissionCode, rolePermissionCodes);
                wrapper.eq(SysPermission::getPermissionType, 3);
                rolePermissions = permissionMapper.selectList(wrapper);
            }

            // 获取用户直接权限
            List<String> userPermissionCodes = userPermissionMapper.selectPermissionCodesByUserId(userId);
            List<SysPermission> userPermissions = new ArrayList<>();
            if (!CollectionUtils.isEmpty(userPermissionCodes)) {
                LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
                wrapper.in(SysPermission::getPermissionCode, userPermissionCodes);
                wrapper.eq(SysPermission::getPermissionType, 3);
                userPermissions = permissionMapper.selectList(wrapper);
            }

            // 获取所有权限
            List<SysPermission> allPermissions = getAllPermissions();

            result.put("rolePermissions", rolePermissions);
            result.put("userPermissions", userPermissions);
            result.put("allPermissions", allPermissions);
            return result;
        } catch (Exception e) {
            log.error("获取用户权限详情失败，userId: {}", userId, e);
            result.put("rolePermissions", new ArrayList<>());
            result.put("userPermissions", new ArrayList<>());
            result.put("allPermissions", new ArrayList<>());
            return result;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserPermissions(Long userId, List<Long> permissionIds) {
        try {
            if (userId == null) {
                throw new RuntimeException("用户ID不能为空");
            }

            // 检查用户是否存在
            SysUser user = userMapper.selectById(userId);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }

            // 超级管理员不允许修改权限
            if (user.getRoleId() != null && user.getRoleId() == 6) {
                throw new RuntimeException("超级管理员的权限不允许修改");
            }

            // 删除用户现有的直接权限
            LambdaQueryWrapper<SysUserPermission> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(SysUserPermission::getUserId, userId);
            userPermissionMapper.delete(deleteWrapper);

            // 添加新的权限
            if (permissionIds != null && !permissionIds.isEmpty()) {
                // 验证权限ID是否存在
                LambdaQueryWrapper<SysPermission> permissionWrapper = new LambdaQueryWrapper<>();
                permissionWrapper.in(SysPermission::getId, permissionIds);
                permissionWrapper.eq(SysPermission::getPermissionType, 3);
                List<SysPermission> validPermissions = permissionMapper.selectList(permissionWrapper);
                
                if (validPermissions.size() != permissionIds.size()) {
                    log.warn("部分权限ID无效，userId: {}, permissionIds: {}", userId, permissionIds);
                }

                // 插入新的用户权限关联
                for (SysPermission permission : validPermissions) {
                    SysUserPermission userPermission = new SysUserPermission();
                    userPermission.setUserId(userId);
                    userPermission.setPermissionId(permission.getId());
                    userPermissionMapper.insert(userPermission);
                }
            }

            log.info("更新用户权限成功，userId: {}, permissionIds: {}", userId, permissionIds);
        } catch (Exception e) {
            log.error("更新用户权限失败，userId: {}, permissionIds: {}", userId, permissionIds, e);
            throw e;
        }
    }
}


