package com.cdiom.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.model.LoginLog;

import java.util.List;

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
     * 按与列表相同的条件查询日志，用于导出（最多 10000 条，按登录时间倒序）
     */
    List<LoginLog> listLogsForExport(String keyword, Long userId, Integer status);

    int EXPORT_MAX_ROWS = 10_000;

    /**
     * 根据ID查询登录日志
     */
    LoginLog getLogById(Long id);

    /**
     * 保存登录日志
     */
    void saveLog(LoginLog log);
}













