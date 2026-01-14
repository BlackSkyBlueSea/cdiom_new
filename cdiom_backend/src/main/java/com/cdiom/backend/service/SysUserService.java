package com.cdiom.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.model.SysUser;

/**
 * 系统用户服务接口
 * 
 * @author cdiom
 */
public interface SysUserService {

    /**
     * 分页查询用户列表
     */
    Page<SysUser> getUserList(Integer page, Integer size, String keyword, Long roleId, Integer status);

    /**
     * 根据ID查询用户
     */
    SysUser getUserById(Long id);

    /**
     * 创建用户
     */
    SysUser createUser(SysUser user);

    /**
     * 更新用户
     */
    SysUser updateUser(SysUser user);

    /**
     * 删除用户（逻辑删除）
     */
    void deleteUser(Long id);

    /**
     * 更新用户状态
     */
    void updateUserStatus(Long id, Integer status);

    /**
     * 解锁用户
     */
    void unlockUser(Long id);

    /**
     * 根据用户名查询用户
     */
    SysUser getUserByUsername(String username);

    /**
     * 获取已删除用户列表（逻辑删除）
     */
    Page<SysUser> getDeletedUserList(Integer page, Integer size, String keyword);

    /**
     * 恢复用户（将deleted从1改为0）
     */
    void restoreUser(Long id);

    /**
     * 物理删除用户（真正从数据库删除）
     */
    void permanentlyDeleteUser(Long id);
}

