package com.cdiom.backend.event;

import com.cdiom.backend.model.InboundRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 占位监听：事务提交后记录日志。后续可新增监听器或改为 ApplicationListener 链。
 */
@Slf4j
@Component
public class UnqualifiedInboundRecordedListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUnqualifiedRecorded(UnqualifiedInboundRecordedEvent event) {
        InboundRecord r = event.getInboundRecord();
        log.info("不合格入库已提交（可扩展：处置/退货/销毁） recordNumber={}, dispositionCode={}",
                r.getRecordNumber(), r.getDispositionCode());
    }
}
