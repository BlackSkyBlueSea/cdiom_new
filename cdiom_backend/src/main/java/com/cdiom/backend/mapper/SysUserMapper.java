package com.cdiom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdiom.backend.model.SysUser;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}

