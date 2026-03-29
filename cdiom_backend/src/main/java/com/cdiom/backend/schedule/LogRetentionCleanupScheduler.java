package com.cdiom.backend.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cdiom.backend.mapper.LoginLogMapper;
import com.cdiom.backend.mapper.OperationLogMapper;
import com.cdiom.backend.model.LoginLog;
import com.cdiom.backend.model.OperationLog;
import com.cdiom.backend.util.SystemConfigUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 按系统参数 {@code log_retention_years} 清理过期登录日志与操作日志（物理删除）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogRetentionCleanupScheduler {

    private final SystemConfigUtil systemConfigUtil;
    private final LoginLogMapper loginLogMapper;
    private final OperationLogMapper operationLogMapper;

    /** 每日凌晨 3 点执行 */
    @Scheduled(cron = "0 0 3 * * ?")
    public void purgeOldLogs() {
        int years = systemConfigUtil.getLogRetentionYears();
        if (years < 1) {
            years = 1;
        }
        LocalDateTime threshold = LocalDateTime.now().minusYears(years);

        LambdaQueryWrapper<LoginLog> w1 = new LambdaQueryWrapper<>();
        w1.lt(LoginLog::getLoginTime, threshold);
        int n1 = loginLogMapper.delete(w1);

        LambdaQueryWrapper<OperationLog> w2 = new LambdaQueryWrapper<>();
        w2.lt(OperationLog::getOperationTime, threshold);
        int n2 = operationLogMapper.delete(w2);

        log.info("日志保留清理：删除登录日志 {} 条、操作日志 {} 条（保留最近 {} 年内的记录）", n1, n2, years);
    }
}
