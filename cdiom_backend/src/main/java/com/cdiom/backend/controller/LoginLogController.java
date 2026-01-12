package com.cdiom.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.model.LoginLog;
import com.cdiom.backend.service.LoginLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 登录日志控制器
 * 
 * @author cdiom
 */
@RestController
@RequestMapping("/api/v1/login-logs")
@RequiredArgsConstructor
@RequiresPermission("log:login:view")
public class LoginLogController {

    private final LoginLogService loginLogService;

    /**
     * 分页查询登录日志列表
     */
    @GetMapping
    public Result<Page<LoginLog>> getLogList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status) {
        Page<LoginLog> logPage = loginLogService.getLogList(page, size, keyword, userId, status);
        return Result.success(logPage);
    }

    /**
     * 根据ID查询登录日志
     */
    @GetMapping("/{id}")
    public Result<LoginLog> getLogById(@PathVariable Long id) {
        LoginLog log = loginLogService.getLogById(id);
        return Result.success(log);
    }
}



