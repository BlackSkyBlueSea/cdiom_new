package com.cdiom.backend.event;

import com.cdiom.backend.model.InboundRecord;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 不合格入库记录已成功写入数据库后发布（事务提交后监听）。
 * 后续可在此挂接：退货出库单、销毁记录、供应商通知等，无需改核心入库逻辑。
 */
@Getter
public class UnqualifiedInboundRecordedEvent extends ApplicationEvent {

    private final InboundRecord inboundRecord;

    public UnqualifiedInboundRecordedEvent(Object source, InboundRecord inboundRecord) {
        super(source);
        this.inboundRecord = inboundRecord;
    }
}
