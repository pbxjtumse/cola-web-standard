package com.xjtu.iron.cola.web;

import com.xjtu.iron.cola.web.tracing.ITraceSpan;
import com.xjtu.iron.cola.web.tracing.resolver.TraceErrorResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * DemoBizException 异常解析器。
 *
 * <p>这个类模拟业务项目如何接入自己的异常体系。</p>
 */
@Component
public class DemoBizExceptionTraceErrorResolver implements TraceErrorResolver {

    private static final Logger log = LoggerFactory.getLogger(DemoBizExceptionTraceErrorResolver.class);

    @Override
    public boolean supports(Throwable throwable) {
        return throwable instanceof DemoBizException;
    }

    @Override
    public void resolve(Throwable throwable, ITraceSpan span) {
        DemoBizException exception = (DemoBizException) throwable;

        span.tag("error.type", "BUSINESS");
        span.tag("error.code", exception.getCode());
        span.tag("error.biz", true);
        log.info("DemoBizException resolved, code={}", exception.getCode());
    }
}