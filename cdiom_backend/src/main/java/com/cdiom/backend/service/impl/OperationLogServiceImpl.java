package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.mapper.OperationLogMapper;
import com.cdiom.backend.model.OperationLog;
import com.cdiom.backend.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 操作日志服务实现类
 * 
 * @author cdiom
 */
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public Page<OperationLog> getLogList(Integer page, Integer size, String keyword, Long userId, String module, String operationType, Integer status) {
        Page<OperationLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(OperationLog::getUsername, keyword)
                    .or().like(OperationLog::getModule, keyword)
                    .or().like(OperationLog::getOperationContent, keyword));
        }
        
        if (userId != null) {
            wrapper.eq(OperationLog::getUserId, userId);
        }
        
        if (StringUtils.hasText(module)) {
            wrapper.eq(OperationLog::getModule, module);
        }
        
        if (StringUtils.hasText(operationType)) {
            wrapper.eq(OperationLog::getOperationType, operationType);
        }
        
        if (status != null) {
            wrapper.eq(OperationLog::getStatus, status);
        }
        
        wrapper.orderByDesc(OperationLog::getOperationTime);
        
        return operationLogMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public OperationLog getLogById(Long id) {
        return operationLogMapper.selectById(id);
    }

    @Override
    public void saveLog(OperationLog log) {
        if (log.getOperationTime() == null) {
            log.setOperationTime(LocalDateTime.now());
        }
        operationLogMapper.insert(log);
    }
}









