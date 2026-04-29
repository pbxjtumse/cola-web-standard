package com.xjtu.iron.cola.web.tracing;


import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.io.IOException;

@Component
public class TraceFilter implements Filter {

    @Autowired
    private TraceService traceService;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        TraceSpan span = traceService.startSpan(((HttpServletRequest) req).getRequestURI());

        try {
            TraceMdc.inject(traceService);
            chain.doFilter(req, res);
        } finally {
            span.end();
            TraceMdc.clear();
        }
    }
}
