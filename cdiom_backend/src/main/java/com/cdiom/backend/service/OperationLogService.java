package com.cdiom.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.model.OperationLog;

import java.util.List;

/**
 * 操作日志服务接口
 * 
 * @author cdiom
 */
public interface OperationLogService {

    /**
     * 分页查询操作日志列表
     */
    Page<OperationLog> getLogList(Integer page, Integer size, String keyword, Long userId, String module, String operationType, Integer status);

    /**
     * 按与列表相同的条件查询日志，用于导出（最多 10000 条，按操作时间倒序）
     */
    List<OperationLog> listLogsForExport(String keyword, Long userId, String module, String operationType, Integer status);

    int EXPORT_MAX_ROWS = 10_000;

    /**
     * 根据ID查询操作日志
     */
    OperationLog getLogById(Long id);

    /**
     * 保存操作日志
     */
    void saveLog(OperationLog log);
}













