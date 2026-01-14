package com.cdiom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdiom.backend.model.SysUserPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户权限关联Mapper接口
 * 
 * @author cdiom
 */
@Mapper
public interface SysUserPermissionMapper extends BaseMapper<SysUserPermission> {

    /**
     * 根据用户ID查询权限代码列表（用户直接拥有的权限）
     */
    @Select("SELECT p.permission_code FROM sys_permission p " +
            "INNER JOIN sys_user_permission up ON p.id = up.permission_id " +
            "WHERE up.user_id = #{userId} AND p.permission_type = 3")
    List<String> selectPermissionCodesByUserId(Long userId);

    /**
     * 根据用户ID查询权限ID列表（用户直接拥有的权限）
     */
    @Select("SELECT up.permission_id FROM sys_user_permission up " +
            "INNER JOIN sys_permission p ON up.permission_id = p.id " +
            "WHERE up.user_id = #{userId} AND p.permission_type = 3")
    List<Long> selectPermissionIdsByUserId(Long userId);
}


