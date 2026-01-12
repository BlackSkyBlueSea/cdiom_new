package com.cdiom.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.model.SysRole;

/**
 * 系统角色服务接口
 * 
 * @author cdiom
 */
public interface SysRoleService {

    /**
     * 分页查询角色列表
     */
    Page<SysRole> getRoleList(Integer page, Integer size, String keyword, Integer status);

    /**
     * 根据ID查询角色
     */
    SysRole getRoleById(Long id);

    /**
     * 创建角色
     */
    SysRole createRole(SysRole role);

    /**
     * 更新角色
     */
    SysRole updateRole(SysRole role);

    /**
     * 删除角色（逻辑删除）
     */
    void deleteRole(Long id);

    /**
     * 更新角色状态
     */
    void updateRoleStatus(Long id, Integer status);
}





