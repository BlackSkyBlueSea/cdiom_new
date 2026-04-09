package com.cdiom.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.model.LoginLog;
import com.cdiom.backend.model.SysUser;
import com.cdiom.backend.service.AuthService;
import com.cdiom.backend.service.ExcelExportService;
import com.cdiom.backend.service.LoginLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    private final AuthService authService;
    private final ExcelExportService excelExportService;

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
     * 导出登录日志（Excel，与当前筛选条件一致，最多 10000 条）
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportLoginLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status) {
        try {
            SysUser currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                throw new RuntimeException("未登录或登录已过期");
            }
            List<LoginLog> logs = loginLogService.listLogsForExport(keyword, userId, status);
            byte[] excelBytes = excelExportService.exportLoginLogList(logs, currentUser);
            String fileName = "登录日志_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", encodedFileName);
            headers.setContentLength(excelBytes.length);
            return ResponseEntity.ok().headers(headers).body(excelBytes);
        } catch (Exception e) {
            throw new RuntimeException("导出失败: " + e.getMessage(), e);
        }
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



