package com.cdiom.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.model.OperationLog;
import com.cdiom.backend.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志控制器
 * 
 * @author cdiom
 */
@RestController
@RequestMapping("/api/v1/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    /**
     * 分页查询操作日志列表
     */
    @GetMapping
    public Result<Page<OperationLog>> getLogList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) Integer status) {
        Page<OperationLog> logPage = operationLogService.getLogList(page, size, keyword, userId, module, operationType, status);
        return Result.success(logPage);
    }

    /**
     * 根据ID查询操作日志
     */
    @GetMapping("/{id}")
    public Result<OperationLog> getLogById(@PathVariable Long id) {
        OperationLog log = operationLogService.getLogById(id);
        return Result.success(log);
    }
}


