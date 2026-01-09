package com.cdiom.backend.service;

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
     * 用户登出
     */
    void logout();
}

