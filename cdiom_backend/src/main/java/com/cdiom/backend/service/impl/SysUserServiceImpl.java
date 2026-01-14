package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.mapper.SysPermissionMapper;
import com.cdiom.backend.mapper.SysUserMapper;
import com.cdiom.backend.mapper.SysUserPermissionMapper;
import com.cdiom.backend.model.SysPermission;
import com.cdiom.backend.model.SysUser;
import com.cdiom.backend.model.SysUserPermission;
import com.cdiom.backend.service.PermissionService;
import com.cdiom.backend.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
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
    private final PermissionService permissionService;
    private final SysPermissionMapper permissionMapper;
    private final SysUserPermissionMapper userPermissionMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Page<SysUser> getUserList(Integer page, Integer size, String keyword, Long roleId, Integer status) {
        Page<SysUser> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        
        if (roleId != null) {
            wrapper.eq(SysUser::getRoleId, roleId);
        }
        
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        
        wrapper.orderByDesc(SysUser::getCreateTime);
        
        return sysUserMapper.selectPage(pageParam, wrapper);
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
            throw new RuntimeException("用户名已存在");
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
                throw new RuntimeException("手机号 " + user.getPhone() + " 已被使用");
            }
        }
        
        // 检查邮箱是否已存在（如果提供了邮箱）
        if (StringUtils.hasText(user.getEmail())) {
            LambdaQueryWrapper<SysUser> emailWrapper = new LambdaQueryWrapper<>();
            emailWrapper.eq(SysUser::getEmail, user.getEmail());
            if (sysUserMapper.selectOne(emailWrapper) != null) {
                throw new RuntimeException("邮箱已被使用");
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
            throw new RuntimeException("用户不存在");
        }
        
        // 如果修改了用户名，检查是否重复
        if (!existUser.getUsername().equals(user.getUsername())) {
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUser::getUsername, user.getUsername());
            if (sysUserMapper.selectOne(wrapper) != null) {
                throw new RuntimeException("用户名已存在");
            }
        }
        
        // 如果修改了手机号，检查是否重复（如果提供了手机号）
        if (StringUtils.hasText(user.getPhone()) && 
            !user.getPhone().equals(existUser.getPhone())) {
            LambdaQueryWrapper<SysUser> phoneWrapper = new LambdaQueryWrapper<>();
            phoneWrapper.eq(SysUser::getPhone, user.getPhone());
            SysUser phoneUser = sysUserMapper.selectOne(phoneWrapper);
            if (phoneUser != null && !phoneUser.getId().equals(user.getId())) {
                throw new RuntimeException("手机号已被使用");
            }
        }
        
        // 如果修改了邮箱，检查是否重复（如果提供了邮箱）
        if (StringUtils.hasText(user.getEmail()) && 
            !user.getEmail().equals(existUser.getEmail())) {
            LambdaQueryWrapper<SysUser> emailWrapper = new LambdaQueryWrapper<>();
            emailWrapper.eq(SysUser::getEmail, user.getEmail());
            SysUser emailUser = sysUserMapper.selectOne(emailWrapper);
            if (emailUser != null && !emailUser.getId().equals(user.getId())) {
                throw new RuntimeException("邮箱已被使用");
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        sysUserMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long id, Integer status) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 超级管理员的状态修改必须通过专门的接口（需要验证码验证）
        // 这里只允许通过 /super-admin/enable 或 /super-admin/disable 接口修改
        if ("super_admin".equals(user.getUsername())) {
            throw new RuntimeException("超级管理员的状态修改需要通过专门的接口，并需要邮箱验证码验证");
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
            throw new RuntimeException("用户不存在");
        }
        user.setLoginFailCount(0);
        user.setLockTime(null);
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
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
        // 注意：由于@TableLogic会自动过滤deleted=1的记录，selectById可能查不到已删除的用户
        // 我们需要直接更新deleted字段
        SysUser user = new SysUser();
        user.setId(id);
        user.setDeleted(0);
        user.setUpdateTime(LocalDateTime.now());
        
        // 使用updateById更新，如果用户不存在，返回0
        int updated = sysUserMapper.updateById(user);
        if (updated == 0) {
            throw new RuntimeException("用户不存在或已被永久删除");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void permanentlyDeleteUser(Long id) {
        // 物理删除：使用原生SQL直接删除（绕过逻辑删除）
        // 注意：此操作不可逆，会真正从数据库删除记录
        int deletedCount = sysUserMapper.permanentlyDeleteById(id);
        
        if (deletedCount == 0) {
            throw new RuntimeException("用户不存在或已被永久删除");
        }
    }
}

