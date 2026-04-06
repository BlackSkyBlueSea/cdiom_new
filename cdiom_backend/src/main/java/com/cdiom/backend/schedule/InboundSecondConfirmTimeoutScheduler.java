package com.cdiom.backend.schedule;

import com.cdiom.backend.service.InboundRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 特殊药品入库「待第二人确认」超时关闭
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InboundSecondConfirmTimeoutScheduler {

    private final InboundRecordService inboundRecordService;

    @Scheduled(cron = "0 */10 * * * ?")
    public void timeoutPendingSecondConfirm() {
        try {
            int n = inboundRecordService.timeoutPendingSecondInbound();
            if (n > 0) {
                log.info("入库第二人确认超时关闭 {} 条", n);
            }
        } catch (Exception e) {
            log.warn("入库第二人确认超时任务异常: {}", e.getMessage());
        }
    }
}
