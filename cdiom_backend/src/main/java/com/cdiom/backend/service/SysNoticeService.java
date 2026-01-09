package com.cdiom.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.model.SysNotice;

/**
 * 系统通知公告服务接口
 * 
 * @author cdiom
 */
public interface SysNoticeService {

    /**
     * 分页查询通知公告列表
     */
    Page<SysNotice> getNoticeList(Integer page, Integer size, String keyword, Integer noticeType, Integer status);

    /**
     * 根据ID查询通知公告
     */
    SysNotice getNoticeById(Long id);

    /**
     * 创建通知公告
     */
    SysNotice createNotice(SysNotice notice);

    /**
     * 更新通知公告
     */
    SysNotice updateNotice(SysNotice notice);

    /**
     * 删除通知公告（逻辑删除）
     */
    void deleteNotice(Long id);

    /**
     * 更新通知公告状态
     */
    void updateNoticeStatus(Long id, Integer status);
}


