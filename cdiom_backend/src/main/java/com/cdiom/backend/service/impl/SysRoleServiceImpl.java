package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.mapper.SysRoleMapper;
import com.cdiom.backend.model.SysRole;
import com.cdiom.backend.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 系统角色服务实现类
 * 
 * @author cdiom
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper sysRoleMapper;

    @Override
    public Page<SysRole> getRoleList(Integer page, Integer size, String keyword, Integer status) {
        Page<SysRole> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysRole::getRoleName, keyword)
                    .or().like(SysRole::getRoleCode, keyword)
                    .or().like(SysRole::getDescription, keyword));
        }
        
        if (status != null) {
            wrapper.eq(SysRole::getStatus, status);
        }
        
        wrapper.orderByDesc(SysRole::getCreateTime);
        
        return sysRoleMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public SysRole getRoleById(Long id) {
        return sysRoleMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRole createRole(SysRole role) {
        // 检查角色代码是否已存在
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getRoleCode, role.getRoleCode());
        if (sysRoleMapper.selectOne(wrapper) != null) {
            throw new RuntimeException("角色代码已存在");
        }
        
        // 设置默认值
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        
        sysRoleMapper.insert(role);
        return role;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRole updateRole(SysRole role) {
        SysRole existRole = sysRoleMapper.selectById(role.getId());
        if (existRole == null) {
            throw new RuntimeException("角色不存在");
        }
        
        // 如果修改了角色代码，检查是否重复
        if (!existRole.getRoleCode().equals(role.getRoleCode())) {
            LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysRole::getRoleCode, role.getRoleCode());
            if (sysRoleMapper.selectOne(wrapper) != null) {
                throw new RuntimeException("角色代码已存在");
            }
        }
        
        role.setUpdateTime(LocalDateTime.now());
        sysRoleMapper.updateById(role);
        return role;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        sysRoleMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRoleStatus(Long id, Integer status) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        role.setStatus(status);
        role.setUpdateTime(LocalDateTime.now());
        sysRoleMapper.updateById(role);
    }
}








