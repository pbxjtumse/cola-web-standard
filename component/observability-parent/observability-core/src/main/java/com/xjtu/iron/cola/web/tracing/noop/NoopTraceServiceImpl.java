package com.xjtu.iron.cola.web.tracing.noop;

import com.xjtu.iron.cola.web.tracing.ITraceService;
import com.xjtu.iron.cola.web.tracing.ITraceSpan;

public class NoopTraceServiceImpl implements ITraceService {

    @Override
    public ITraceSpan startSpan(String spanName) {
        return NoopTraceSpan.INSTANCE;
    }

    @Override
    public String traceId() {
        return "";
    }

    @Override
    public String spanId() {
        return "";
    }
}
