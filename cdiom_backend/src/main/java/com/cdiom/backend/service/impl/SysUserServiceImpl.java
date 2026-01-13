package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.mapper.SysUserMapper;
import com.cdiom.backend.model.SysUser;
import com.cdiom.backend.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 系统用户服务实现类
 * 
 * @author cdiom
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;
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
}

