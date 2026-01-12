package com.cdiom.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.model.SysNotice;
import com.cdiom.backend.service.SysNoticeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 系统通知公告控制器
 * 
 * @author cdiom
 */
@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
@RequiresPermission({"notice:view", "notice:manage"})
public class SysNoticeController {

    private final SysNoticeService sysNoticeService;

    /**
     * 分页查询通知公告列表
     */
    @GetMapping
    public Result<Page<SysNotice>> getNoticeList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer noticeType,
            @RequestParam(required = false) Integer status) {
        Page<SysNotice> noticePage = sysNoticeService.getNoticeList(page, size, keyword, noticeType, status);
        return Result.success(noticePage);
    }

    /**
     * 根据ID查询通知公告
     */
    @GetMapping("/{id}")
    public Result<SysNotice> getNoticeById(@PathVariable Long id) {
        SysNotice notice = sysNoticeService.getNoticeById(id);
        return Result.success(notice);
    }

    /**
     * 创建通知公告
     */
    @PostMapping
    public Result<SysNotice> createNotice(@RequestBody SysNotice notice) {
        try {
            SysNotice createdNotice = sysNoticeService.createNotice(notice);
            return Result.success("创建成功", createdNotice);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新通知公告
     */
    @PutMapping("/{id}")
    public Result<SysNotice> updateNotice(@PathVariable Long id, @RequestBody SysNotice notice) {
        try {
            notice.setId(id);
            SysNotice updatedNotice = sysNoticeService.updateNotice(notice);
            return Result.success("更新成功", updatedNotice);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除通知公告
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteNotice(@PathVariable Long id) {
        try {
            sysNoticeService.deleteNotice(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新通知公告状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateNoticeStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest request) {
        try {
            sysNoticeService.updateNoticeStatus(id, request.getStatus());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Data
    public static class UpdateStatusRequest {
        private Integer status;
    }
}

