package com.cdiom.backend.model.vo;

import com.cdiom.backend.model.InboundRecord;
import lombok.Data;

/**
 * 拆行验收一次提交两条入库明细的返回
 */
@Data
public class InboundSplitResult {
    private InboundRecord qualifiedRecord;
    private InboundRecord unqualifiedRecord;
}
