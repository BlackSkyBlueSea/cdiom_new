package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.mapper.SysNoticeMapper;
import com.cdiom.backend.model.SysNotice;
import com.cdiom.backend.service.SysNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 系统通知公告服务实现类
 * 
 * @author cdiom
 */
@Service
@RequiredArgsConstructor
public class SysNoticeServiceImpl implements SysNoticeService {

    private final SysNoticeMapper sysNoticeMapper;

    @Override
    public Page<SysNotice> getNoticeList(Integer page, Integer size, String keyword, Integer noticeType, Integer status) {
        Page<SysNotice> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysNotice::getNoticeTitle, keyword)
                    .or().like(SysNotice::getNoticeContent, keyword));
        }
        
        if (noticeType != null) {
            wrapper.eq(SysNotice::getNoticeType, noticeType);
        }
        
        if (status != null) {
            wrapper.eq(SysNotice::getStatus, status);
        }
        
        wrapper.orderByDesc(SysNotice::getCreateTime);
        
        return sysNoticeMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public SysNotice getNoticeById(Long id) {
        return sysNoticeMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysNotice createNotice(SysNotice notice) {
        // 设置默认值
        if (notice.getNoticeType() == null) {
            notice.setNoticeType(1);
        }
        if (notice.getStatus() == null) {
            notice.setStatus(1);
        }
        notice.setCreateTime(LocalDateTime.now());
        notice.setUpdateTime(LocalDateTime.now());
        
        sysNoticeMapper.insert(notice);
        return notice;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysNotice updateNotice(SysNotice notice) {
        SysNotice existNotice = sysNoticeMapper.selectById(notice.getId());
        if (existNotice == null) {
            throw new RuntimeException("通知公告不存在");
        }
        
        notice.setUpdateTime(LocalDateTime.now());
        sysNoticeMapper.updateById(notice);
        return notice;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotice(Long id) {
        sysNoticeMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNoticeStatus(Long id, Integer status) {
        SysNotice notice = sysNoticeMapper.selectById(id);
        if (notice == null) {
            throw new RuntimeException("通知公告不存在");
        }
        notice.setStatus(status);
        notice.setUpdateTime(LocalDateTime.now());
        sysNoticeMapper.updateById(notice);
    }
}






