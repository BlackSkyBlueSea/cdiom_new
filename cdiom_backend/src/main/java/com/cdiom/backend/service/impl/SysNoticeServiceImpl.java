package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.common.exception.ServiceException;
import com.cdiom.backend.mapper.SysNoticeMapper;
import com.cdiom.backend.model.SysNotice;
import com.cdiom.backend.model.SysUser;
import com.cdiom.backend.service.AuthService;
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
    private final AuthService authService;

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
        // 获取当前用户
        SysUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("未登录或登录已过期");
        }
        
        // 设置默认值
        if (notice.getNoticeType() == null) {
            notice.setNoticeType(1);
        }
        if (notice.getStatus() == null) {
            notice.setStatus(1);
        }
        notice.setCreateBy(currentUser.getId());
        notice.setCreateTime(LocalDateTime.now());
        notice.setUpdateTime(LocalDateTime.now());
        
        sysNoticeMapper.insert(notice);
        return notice;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysNotice updateNotice(SysNotice notice) {
        // 获取当前用户
        SysUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("未登录或登录已过期");
        }
        
        SysNotice existNotice = sysNoticeMapper.selectById(notice.getId());
        if (existNotice == null) {
            throw new ServiceException("通知公告不存在");
        }
        
        // 检查权限：系统管理员（roleId=1）和超级管理员（roleId=6）可以修改所有，其他用户只能修改自己创建的
        Long currentUserId = currentUser.getId();
        Long currentUserRoleId = currentUser.getRoleId();
        boolean isAdmin = currentUserRoleId != null && (currentUserRoleId == 1L || currentUserRoleId == 6L);
        
        if (!isAdmin && (existNotice.getCreateBy() == null || !existNotice.getCreateBy().equals(currentUserId))) {
            throw new ServiceException("无权限修改他人的通知公告");
        }
        
        notice.setUpdateTime(LocalDateTime.now());
        sysNoticeMapper.updateById(notice);
        return notice;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotice(Long id) {
        // 获取当前用户
        SysUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("未登录或登录已过期");
        }
        
        SysNotice existNotice = sysNoticeMapper.selectById(id);
        if (existNotice == null) {
            throw new ServiceException("通知公告不存在");
        }
        
        // 检查权限：系统管理员（roleId=1）和超级管理员（roleId=6）可以删除所有，其他用户只能删除自己创建的
        Long currentUserId = currentUser.getId();
        Long currentUserRoleId = currentUser.getRoleId();
        boolean isAdmin = currentUserRoleId != null && (currentUserRoleId == 1L || currentUserRoleId == 6L);
        
        if (!isAdmin && (existNotice.getCreateBy() == null || !existNotice.getCreateBy().equals(currentUserId))) {
            throw new ServiceException("无权限删除他人的通知公告");
        }
        
        sysNoticeMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNoticeStatus(Long id, Integer status) {
        // 获取当前用户
        SysUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("未登录或登录已过期");
        }
        
        SysNotice notice = sysNoticeMapper.selectById(id);
        if (notice == null) {
            throw new ServiceException("通知公告不存在");
        }
        
        // 检查权限：系统管理员（roleId=1）和超级管理员（roleId=6）可以更新所有，其他用户只能更新自己创建的
        Long currentUserId = currentUser.getId();
        Long currentUserRoleId = currentUser.getRoleId();
        boolean isAdmin = currentUserRoleId != null && (currentUserRoleId == 1L || currentUserRoleId == 6L);
        
        if (!isAdmin && (notice.getCreateBy() == null || !notice.getCreateBy().equals(currentUserId))) {
            throw new ServiceException("无权限修改他人的通知公告状态");
        }
        
        notice.setStatus(status);
        notice.setUpdateTime(LocalDateTime.now());
        sysNoticeMapper.updateById(notice);
    }
}






