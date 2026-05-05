package com.xjtu.iron.cola.web;

import com.xjtu.iron.cola.web.tracing.ITraceService;
import com.xjtu.iron.cola.web.tracing.OtelTraceServiceImpl;
import com.xjtu.iron.cola.web.tracing.TraceErrorResolver;
import com.xjtu.iron.cola.web.tracing.TraceTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(ObservabilityProperties.class)
@ConditionalOnProperty(
        prefix = "xy.observability",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ITraceService traceService(ObservabilityProperties properties, List<TraceErrorResolver> errorResolvers) {
        return new OtelTraceServiceImpl(properties.getInstrumentationName(), errorResolvers);
    }

    @Bean
    @ConditionalOnMissingBean
    public TraceTemplate traceTemplate(ITraceService traceService) {
        return new TraceTemplate(traceService);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "xy.observability",
            name = "trace-aspect-enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public TraceAspect traceAspect(ITraceService traceService) {
        return new TraceAspect(traceService);
    }
}