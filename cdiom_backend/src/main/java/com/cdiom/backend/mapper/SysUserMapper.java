package com.cdiom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdiom.backend.model.SysUser;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 系统用户Mapper接口
 * 
 * @author cdiom
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    
    /**
     * 物理删除用户（真正从数据库删除）
     * 注意：此方法会绕过MyBatis-Plus的逻辑删除机制
     */
    @Delete("DELETE FROM sys_user WHERE id = #{id}")
    int permanentlyDeleteById(@Param("id") Long id);

    /**
     * 将逻辑删除的用户恢复为未删除（绕过 BaseMapper.updateById 对 @TableLogic 的 WHERE deleted=0 限制）
     */
    @Update("UPDATE sys_user SET deleted = 0, update_time = #{updateTime} WHERE id = #{id} AND deleted = 1")
    int restoreLogicallyDeletedById(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);
    
    /**
     * 查询已删除的用户列表（包括逻辑删除的记录）
     * 注意：此方法绕过@TableLogic的自动过滤
     */
    @Select("<script>" +
            "SELECT * FROM sys_user WHERE deleted = 1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (username LIKE CONCAT('%', #{keyword}, '%') OR phone LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY update_time DESC " +
            "LIMIT #{offset}, #{limit} " +
            "</script>")
    java.util.List<SysUser> selectDeletedUsersList(@Param("keyword") String keyword, @Param("offset") Long offset, @Param("limit") Long limit);
    
    /**
     * 统计已删除用户总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM sys_user WHERE deleted = 1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (username LIKE CONCAT('%', #{keyword}, '%') OR phone LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "</script>")
    Long countDeletedUsers(@Param("keyword") String keyword);

    /**
     * 具备出库审核权限的用户（outbound:approve 或 outbound:approve:special），含超级管理员角色；用于特殊药品第二审批人候选
     */
    @Select("""
            SELECT u.* FROM sys_user u
            WHERE u.deleted = 0 AND u.status = 1
            AND (
                u.role_id = 6
                OR EXISTS (
                    SELECT 1 FROM sys_role_permission rp
                    INNER JOIN sys_permission p ON p.id = rp.permission_id
                    WHERE rp.role_id = u.role_id
                    AND p.permission_code IN ('outbound:approve', 'outbound:approve:special')
                )
                OR EXISTS (
                    SELECT 1 FROM sys_user_permission up
                    INNER JOIN sys_permission p ON p.id = up.permission_id
                    WHERE up.user_id = u.id
                    AND p.permission_code IN ('outbound:approve', 'outbound:approve:special')
                )
            )
            ORDER BY u.username ASC
            """)
    java.util.List<SysUser> selectUsersWithOutboundApprovePermissions();
}

