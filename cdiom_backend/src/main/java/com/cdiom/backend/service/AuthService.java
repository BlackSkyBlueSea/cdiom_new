package com.cdiom.backend.service;

import com.cdiom.backend.model.AdminContactInfo;
import com.cdiom.backend.model.SysUser;

/**
 * 认证服务接口
 * 
 * @author cdiom
 */
public interface AuthService {

    /**
     * 用户登录
     * @return 返回数组，第一个元素是token，第二个元素是用户信息
     */
    Object[] login(String username, String password, String ip, String browser, String os);

    /**
     * 获取当前登录用户信息
     */
    SysUser getCurrentUser();

    /**
     * 从安全上下文读取当前用户 ID（不查库）。逻辑删除后仍可从 JWT 解析，用于禁止「对自己」等操作。
     */
    Long getCurrentUserId();

    /**
     * 获取系统管理员联系信息（取首个启用的系统管理员账号，供用户申请修改资料时联系）
     */
    AdminContactInfo getAdminContactForUsers();

    /**
     * 用户登出
     */
    void logout();
}

