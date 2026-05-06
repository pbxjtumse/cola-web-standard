package com.xjtu.iron.cola.web.tracing.noop;

import com.xjtu.iron.cola.web.enums.TraceProviderType;
import com.xjtu.iron.cola.web.tracing.ITraceService;
import com.xjtu.iron.cola.web.tracing.provider.TraceProvider;
import com.xjtu.iron.cola.web.tracing.context.TraceProviderContext;

public class NoopTraceProviderImpl implements TraceProvider {

    @Override
    public TraceProviderType type() {
        return TraceProviderType.NOOP;
    }

    @Override
    public ITraceService createTraceService(TraceProviderContext context) {
        return new NoopTraceServiceImpl();
    }
}
