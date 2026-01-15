package com.cdiom.backend.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体类
 * 
 * @author cdiom
 */
@Data
@TableName("sys_user")
public class SysUser {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱地址
     */
    private String email;

    /**
     * 密码（BCrypt加密）
     */
    private String password;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 状态：0-禁用/1-正常
     */
    private Integer status;

    /**
     * 锁定时间
     */
    private LocalDateTime lockTime;

    /**
     * 登录失败次数
     */
    private Integer loginFailCount;

    /**
     * 最后登录失败时间
     */
    private LocalDateTime lastLoginFailTime;

    /**
     * 创建人ID
     */
    private Long createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0-未删除/1-已删除
     */
    @TableLogic
    private Integer deleted;
}

