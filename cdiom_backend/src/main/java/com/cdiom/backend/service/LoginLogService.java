package com.cdiom.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.model.LoginLog;

/**
 * 登录日志服务接口
 * 
 * @author cdiom
 */
public interface LoginLogService {

    /**
     * 分页查询登录日志列表
     */
    Page<LoginLog> getLogList(Integer page, Integer size, String keyword, Long userId, Integer status);

    /**
     * 根据ID查询登录日志
     */
    LoginLog getLogById(Long id);

    /**
     * 保存登录日志
     */
    void saveLog(LoginLog log);
}








