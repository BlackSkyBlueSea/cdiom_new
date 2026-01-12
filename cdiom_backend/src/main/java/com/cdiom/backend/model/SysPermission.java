package com.cdiom.backend.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统权限实体类
 * 
 * @author cdiom
 */
@Data
@TableName("sys_permission")
public class SysPermission {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * 权限代码
     */
    private String permissionCode;

    /**
     * 权限类型：1-菜单/2-按钮/3-接口
     */
    private Integer permissionType;

    /**
     * 父级ID
     */
    private Long parentId;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 是否必需：0-否/1-是
     */
    private Integer isRequired;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}



