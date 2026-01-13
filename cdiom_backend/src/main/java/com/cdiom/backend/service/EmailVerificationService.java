package com.cdiom.backend.service;

/**
 * 邮箱验证码服务接口
 * 
 * @author cdiom
 */
public interface EmailVerificationService {

    /**
     * 发送验证码到指定邮箱
     * 
     * @param email 目标邮箱地址
     * @return 验证码（用于测试，生产环境应返回null）
     */
    String sendVerificationCode(String email);

    /**
     * 验证验证码
     * 
     * @param email 邮箱地址
     * @param code 验证码
     * @return 是否验证通过
     */
    boolean verifyCode(String email, String code);

    /**
     * 清除指定邮箱的验证码（验证成功后调用）
     * 
     * @param email 邮箱地址
     */
    void clearCode(String email);
}

