package com.cdiom.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.model.SysRole;
import com.cdiom.backend.service.SysRoleService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 系统角色管理控制器
 * 
 * @author cdiom
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@RequiresPermission("role:manage")
public class SysRoleController {

    private final SysRoleService sysRoleService;

    /**
     * 分页查询角色列表
     */
    @GetMapping
    public Result<Page<SysRole>> getRoleList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        Page<SysRole> rolePage = sysRoleService.getRoleList(page, size, keyword, status);
        return Result.success(rolePage);
    }

    /**
     * 根据ID查询角色
     */
    @GetMapping("/{id}")
    public Result<SysRole> getRoleById(@PathVariable Long id) {
        SysRole role = sysRoleService.getRoleById(id);
        return Result.success(role);
    }

    /**
     * 创建角色
     */
    @PostMapping
    public Result<SysRole> createRole(@RequestBody SysRole role) {
        try {
            SysRole createdRole = sysRoleService.createRole(role);
            return Result.success("创建成功", createdRole);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    public Result<SysRole> updateRole(@PathVariable Long id, @RequestBody SysRole role) {
        try {
            role.setId(id);
            SysRole updatedRole = sysRoleService.updateRole(role);
            return Result.success("更新成功", updatedRole);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        try {
            sysRoleService.deleteRole(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新角色状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateRoleStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest request) {
        try {
            sysRoleService.updateRoleStatus(id, request.getStatus());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Data
    public static class UpdateStatusRequest {
        private Integer status;
    }
}

