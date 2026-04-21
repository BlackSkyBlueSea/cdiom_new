package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.common.exception.ServiceException;
import com.cdiom.backend.mapper.SysPermissionMapper;
import com.cdiom.backend.mapper.SysUserMapper;
import com.cdiom.backend.mapper.SysUserPermissionMapper;
import com.cdiom.backend.model.SysPermission;
import com.cdiom.backend.model.SysUser;
import com.cdiom.backend.model.SysUserPermission;
import com.cdiom.backend.service.AuthService;
import com.cdiom.backend.service.PermissionService;
import com.cdiom.backend.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * 系统用户服务实现类
 * 
 * @author cdiom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;
    private final AuthService authService;
    private final PermissionService permissionService;
    private final SysPermissionMapper permissionMapper;
    private final SysUserPermissionMapper userPermissionMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Page<SysUser> getUserList(Integer page, Integer size, String keyword, Long roleId, Integer status, Long permissionId) {
        Page<SysUser> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getPhone, keyword)
                    .or().like(SysUser::getEmail, keyword));
        }
        
        if (roleId != null) {
            wrapper.eq(SysUser::getRoleId, roleId);
        }
        
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }

        if (permissionId != null) {
            // 超级管理员（role_id=6）在业务上拥有全部权限；其余用户按角色权限或直接授权匹配
            wrapper.apply("(sys_user.role_id = 6 OR EXISTS (SELECT 1 FROM sys_user_permission up WHERE up.user_id = sys_user.id AND up.permission_id = {0}) OR EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = sys_user.role_id AND rp.permission_id = {0}))",
                    permissionId);
        }
        
        wrapper.orderByDesc(SysUser::getCreateTime);
        
        Page<SysUser> userPage = sysUserMapper.selectPage(pageParam, wrapper);
        
        // 检查并清除已过期的锁定状态
        LocalDateTime currentTime = LocalDateTime.now();
        List<SysUser> usersToUpdate = new java.util.ArrayList<>();
        
        for (SysUser user : userPage.getRecords()) {
            if (user.getLockTime() != null && currentTime.isAfter(user.getLockTime())) {
                // 锁定时间已过期，清除锁定状态
                user.setLockTime(null);
                user.setLoginFailCount(0);
                user.setLastLoginFailTime(null);
                user.setUpdateTime(LocalDateTime.now());
                usersToUpdate.add(user);
            }
        }
        
        // 批量更新已过期的用户锁定状态
        if (!usersToUpdate.isEmpty()) {
            for (SysUser user : usersToUpdate) {
                sysUserMapper.updateById(user);
            }
            log.info("自动清除 {} 个用户的过期锁定状态", usersToUpdate.size());
        }
        
        return userPage;
    }

    @Override
    public SysUser getUserById(Long id) {
        return sysUserMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser createUser(SysUser user) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, user.getUsername());
        if (sysUserMapper.selectOne(wrapper) != null) {
            throw new ServiceException("用户名已存在");
        }
        
        // 检查手机号是否已存在（如果提供了手机号）
        // 注意：MyBatis-Plus的@TableLogic会自动排除已删除的记录，但数据库唯一索引会检查所有记录
        // 所以即使逻辑删除，手机号也不能重复使用
        if (StringUtils.hasText(user.getPhone())) {
            LambdaQueryWrapper<SysUser> phoneWrapper = new LambdaQueryWrapper<>();
            phoneWrapper.eq(SysUser::getPhone, user.getPhone());
            // MyBatis-Plus会自动添加 deleted=0 的条件，只查询未删除的记录
            SysUser existingUser = sysUserMapper.selectOne(phoneWrapper);
            if (existingUser != null) {
                throw new ServiceException("手机号 " + user.getPhone() + " 已被使用");
            }
        }
        
        // 检查邮箱是否已存在（如果提供了邮箱）
        if (StringUtils.hasText(user.getEmail())) {
            LambdaQueryWrapper<SysUser> emailWrapper = new LambdaQueryWrapper<>();
            emailWrapper.eq(SysUser::getEmail, user.getEmail());
            if (sysUserMapper.selectOne(emailWrapper) != null) {
                throw new ServiceException("邮箱已被使用");
            }
        }
        
        // 加密密码
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        
        // 设置默认值
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        user.setLoginFailCount(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        
        sysUserMapper.insert(user);
        
        // 创建用户后，自动分配该角色的所有权限作为用户的直接权限
        if (user.getRoleId() != null && user.getRoleId() != 6) { // 超级管理员不需要分配
            try {
                Set<String> rolePermissionCodes = permissionService.getPermissionCodesByRoleId(user.getRoleId());
                if (!CollectionUtils.isEmpty(rolePermissionCodes)) {
                    // 根据权限代码查询权限ID
                    LambdaQueryWrapper<SysPermission> permissionWrapper = new LambdaQueryWrapper<>();
                    permissionWrapper.in(SysPermission::getPermissionCode, rolePermissionCodes);
                    permissionWrapper.eq(SysPermission::getPermissionType, 3);
                    List<SysPermission> permissions = permissionMapper.selectList(permissionWrapper);
                    
                    // 为用户分配这些权限
                    for (SysPermission permission : permissions) {
                        SysUserPermission userPermission = new SysUserPermission();
                        userPermission.setUserId(user.getId());
                        userPermission.setPermissionId(permission.getId());
                        userPermissionMapper.insert(userPermission);
                    }
                }
            } catch (Exception e) {
                // 权限分配失败不影响用户创建，只记录日志
                log.warn("为用户自动分配角色权限失败，userId: {}, roleId: {}", user.getId(), user.getRoleId(), e);
            }
        }
        
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser updateUser(SysUser user) {
        SysUser existUser = sysUserMapper.selectById(user.getId());
        if (existUser == null) {
            throw new ServiceException("用户不存在");
        }
        
        // 如果修改了用户名，检查是否重复
        if (!existUser.getUsername().equals(user.getUsername())) {
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUser::getUsername, user.getUsername());
            if (sysUserMapper.selectOne(wrapper) != null) {
                throw new ServiceException("用户名已存在");
            }
        }
        
        // 如果修改了手机号，检查是否重复（如果提供了手机号）
        if (StringUtils.hasText(user.getPhone()) && 
            !user.getPhone().equals(existUser.getPhone())) {
            LambdaQueryWrapper<SysUser> phoneWrapper = new LambdaQueryWrapper<>();
            phoneWrapper.eq(SysUser::getPhone, user.getPhone());
            SysUser phoneUser = sysUserMapper.selectOne(phoneWrapper);
            if (phoneUser != null && !phoneUser.getId().equals(user.getId())) {
                throw new ServiceException("手机号已被使用");
            }
        }
        
        // 如果修改了邮箱，检查是否重复（如果提供了邮箱）
        if (StringUtils.hasText(user.getEmail()) && 
            !user.getEmail().equals(existUser.getEmail())) {
            LambdaQueryWrapper<SysUser> emailWrapper = new LambdaQueryWrapper<>();
            emailWrapper.eq(SysUser::getEmail, user.getEmail());
            SysUser emailUser = sysUserMapper.selectOne(emailWrapper);
            if (emailUser != null && !emailUser.getId().equals(user.getId())) {
                throw new ServiceException("邮箱已被使用");
            }
        }
        
        // 如果修改了密码，需要加密
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            // 不修改密码时，保持原密码
            user.setPassword(existUser.getPassword());
        }
        
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return user;
    }

    private void assertTargetIsNotCurrentUser(Long targetUserId, String message) {
        if (targetUserId == null) {
            return;
        }
        Long selfId = authService.getCurrentUserId();
        if (selfId != null && selfId.equals(targetUserId)) {
            throw new ServiceException(message);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        assertTargetIsNotCurrentUser(id, "不能删除当前登录账号");
        sysUserMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long id, Integer status) {
        assertTargetIsNotCurrentUser(id, "不能修改当前登录账号的状态，请使用其他管理员操作");
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        
        // 超级管理员的状态修改必须通过专门的接口（需要验证码验证）
        // 这里只允许通过 /super-admin/enable 或 /super-admin/disable 接口修改
        if ("super_admin".equals(user.getUsername())) {
            throw new ServiceException("超级管理员的状态修改需要通过专门的接口，并需要邮箱验证码验证");
        }
        
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        // updateById 默认忽略 null 字段，lock_time 无法被清空；必须用 UpdateWrapper 显式置 NULL
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<SysUser> uw = new LambdaUpdateWrapper<>();
        uw.eq(SysUser::getId, id)
                .set(SysUser::getLoginFailCount, 0)
                .set(SysUser::getLockTime, null)
                .set(SysUser::getLastLoginFailTime, null)
                .set(SysUser::getUpdateTime, now);
        sysUserMapper.update(null, uw);
    }

    @Override
    public SysUser getUserByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        return sysUserMapper.selectOne(wrapper);
    }

    @Override
    public Page<SysUser> getDeletedUserList(Integer page, Integer size, String keyword) {
        Page<SysUser> pageParam = new Page<>(page, size);
        
        // 计算偏移量
        Long offset = (long) ((page - 1) * size);
        Long limit = (long) size;
        
        // 查询已删除的用户列表（绕过@TableLogic的自动过滤）
        java.util.List<SysUser> records = sysUserMapper.selectDeletedUsersList(keyword, offset, limit);
        
        // 查询总数
        Long total = sysUserMapper.countDeletedUsers(keyword);
        
        // 设置分页信息
        pageParam.setRecords(records);
        pageParam.setTotal(total);
        
        return pageParam;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreUser(Long id) {
        assertTargetIsNotCurrentUser(id, "不能恢复当前登录账号，请由其他管理员操作");
        // 不能使用 updateById：@TableLogic 会在 WHERE 中追加 deleted=0，已删除行匹配不到，更新行数恒为 0
        LocalDateTime now = LocalDateTime.now();
        int updated = sysUserMapper.restoreLogicallyDeletedById(id, now);
        if (updated == 0) {
            throw new ServiceException("用户不存在或已被永久删除");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void permanentlyDeleteUser(Long id) {
        assertTargetIsNotCurrentUser(id, "不能永久删除当前登录账号");
        // 物理删除：使用原生SQL直接删除（绕过逻辑删除）
        // 注意：此操作不可逆，会真正从数据库删除记录
        int deletedCount = sysUserMapper.permanentlyDeleteById(id);
        
        if (deletedCount == 0) {
            throw new ServiceException("用户不存在或已被永久删除");
        }
    }

    @Override
    public List<SysUser> listActiveUsersForSecondOperatorPick(int limit) {
        int cap = limit <= 0 ? 500 : Math.min(limit, 2000);
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        w.eq(SysUser::getStatus, 1)
                .orderByAsc(SysUser::getUsername);
        List<SysUser> list = sysUserMapper.selectList(w.last("LIMIT " + cap));
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        for (SysUser u : list) {
            u.setPassword(null);
        }
        return list;
    }
}

