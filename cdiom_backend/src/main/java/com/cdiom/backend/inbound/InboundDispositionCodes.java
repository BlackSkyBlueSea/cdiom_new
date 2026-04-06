package com.cdiom.backend.inbound;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 不合格入库处置意向代码（可逐步扩展业务模块，见 {@link com.cdiom.backend.event.UnqualifiedInboundRecordedEvent}）。
 */
public final class InboundDispositionCodes {

    public static final String PENDING = "PENDING";
    /** 拟退回供应商 */
    public static final String RETURN_SUPPLIER = "RETURN_SUPPLIER";
    /** 拟销毁 */
    public static final String DESTROY = "DESTROY";
    /** 暂存待处理 */
    public static final String HOLD = "HOLD";
    public static final String OTHER = "OTHER";

    private static final Set<String> ALL = Set.of(
            PENDING, RETURN_SUPPLIER, DESTROY, HOLD, OTHER
    );

    private static final Map<String, String> LABELS;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(PENDING, "待处理");
        m.put(RETURN_SUPPLIER, "拟退回供应商");
        m.put(DESTROY, "拟销毁");
        m.put(HOLD, "暂存待处理");
        m.put(OTHER, "其他");
        LABELS = Collections.unmodifiableMap(m);
    }

    private InboundDispositionCodes() {
    }

    public static boolean isValid(String code) {
        return code != null && ALL.contains(code);
    }

    public static Map<String, String> labels() {
        return LABELS;
    }
}
