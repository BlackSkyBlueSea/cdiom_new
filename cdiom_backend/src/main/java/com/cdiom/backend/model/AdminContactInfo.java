package com.cdiom.backend.model;

import lombok.Data;

/**
 * 面向普通用户的系统管理员联系信息（用于个人中心提示，不含敏感字段）
 */
@Data
public class AdminContactInfo {

    private String adminUsername;
    private String phone;
    private String email;
}
