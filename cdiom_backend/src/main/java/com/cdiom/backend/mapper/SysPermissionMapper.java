package com.cdiom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdiom.backend.model.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统权限Mapper接口
 * 
 * @author cdiom
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 根据角色ID查询权限代码列表
     */
    @Select("SELECT p.permission_code FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON p.id = rp.permission_id " +
            "WHERE rp.role_id = #{roleId} AND p.permission_type = 3")
    List<String> selectPermissionCodesByRoleId(Long roleId);

    /**
     * 根据用户ID查询有效权限代码。
     * permission_customized=1：仅以 sys_user_permission 为准（与用户管理中勾选一致，可少于角色默认）；
     * 否则：角色权限 ∪ 用户直接权限（历史并集行为）。
     */
    @Select("SELECT DISTINCT p.permission_code FROM sys_permission p " +
            "INNER JOIN sys_user u ON u.id = #{userId} " +
            "WHERE p.permission_type = 3 AND ( " +
            "  (COALESCE(u.permission_customized, 0) = 1 " +
            "   AND p.id IN (SELECT up.permission_id FROM sys_user_permission up WHERE up.user_id = #{userId})) " +
            "  OR (COALESCE(u.permission_customized, 0) = 0 " +
            "   AND (p.id IN (SELECT rp.permission_id FROM sys_role_permission rp WHERE rp.role_id = u.role_id) " +
            "        OR p.id IN (SELECT up.permission_id FROM sys_user_permission up WHERE up.user_id = #{userId}))) " +
            ")")
    List<String> selectAllPermissionCodesByUserId(Long userId);
}




