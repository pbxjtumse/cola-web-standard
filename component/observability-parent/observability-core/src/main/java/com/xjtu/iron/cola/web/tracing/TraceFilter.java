package com.xjtu.iron.cola.web.tracing;


import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.io.IOException;

@Component
public class TraceFilter implements Filter {

    @Autowired
    private ITraceService ITraceService;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        TraceSpan span = ITraceService.startSpan(((HttpServletRequest) req).getRequestURI());

        try {
            TraceMdc.inject(ITraceService);
            chain.doFilter(req, res);
        } finally {
            span.end();
            TraceMdc.clear();
        }
    }
}
