package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.mapper.LoginLogMapper;
import com.cdiom.backend.model.LoginLog;
import com.cdiom.backend.service.LoginLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 登录日志服务实现类
 * 
 * @author cdiom
 */
@Service
@RequiredArgsConstructor
public class LoginLogServiceImpl implements LoginLogService {

    private final LoginLogMapper loginLogMapper;

    @Override
    public Page<LoginLog> getLogList(Integer page, Integer size, String keyword, Long userId, Integer status) {
        Page<LoginLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(LoginLog::getUsername, keyword)
                    .or().like(LoginLog::getIp, keyword)
                    .or().like(LoginLog::getLocation, keyword));
        }
        
        if (userId != null) {
            wrapper.eq(LoginLog::getUserId, userId);
        }
        
        if (status != null) {
            wrapper.eq(LoginLog::getStatus, status);
        }
        
        wrapper.orderByDesc(LoginLog::getLoginTime);
        
        return loginLogMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public LoginLog getLogById(Long id) {
        return loginLogMapper.selectById(id);
    }

    @Override
    public void saveLog(LoginLog log) {
        if (log.getLoginTime() == null) {
            log.setLoginTime(LocalDateTime.now());
        }
        loginLogMapper.insert(log);
    }
}






