package com.xjtu.iron.cola.web.tracing.impl;

import com.xjtu.iron.cola.web.enums.TraceProviderType;
import com.xjtu.iron.cola.web.tracing.ITraceService;
import com.xjtu.iron.cola.web.tracing.OtelTraceServiceImpl;
import com.xjtu.iron.cola.web.tracing.provider.TraceProvider;
import com.xjtu.iron.cola.web.tracing.context.TraceProviderContext;

public class OtelTraceProviderImpl implements TraceProvider {

    @Override
    public TraceProviderType type() {
        return TraceProviderType.OTEL;
    }

    @Override
    public ITraceService createTraceService(TraceProviderContext context) {
        return new OtelTraceServiceImpl(
                context.getInstrumentationScopeName(),
                context.getErrorResolvers()
        );
    }

}
