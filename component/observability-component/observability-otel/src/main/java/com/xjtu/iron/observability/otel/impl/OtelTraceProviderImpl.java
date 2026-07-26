package com.xjtu.iron.observability.otel.impl;

import com.xjtu.iron.observability.api.enums.TraceProviderType;
import com.xjtu.iron.observability.api.tracing.ITraceService;
import com.xjtu.iron.observability.otel.OtelTraceServiceImpl;
import com.xjtu.iron.observability.api.tracing.provider.TraceProvider;
import com.xjtu.iron.observability.api.tracing.context.TraceProviderContext;

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
