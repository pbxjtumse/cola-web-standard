package com.xjtu.iron.cola.web;

import com.xjtu.iron.cola.web.tracing.Trace;
import com.xjtu.iron.cola.web.tracing.TraceTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {
    private final TraceTemplate traceTemplate;

    public DemoController(TraceTemplate traceTemplate) {
        this.traceTemplate = traceTemplate;
    }

    @GetMapping("/hello")
    @Trace("demo.hello")
    public String hello() {
        return "hello observability";
    }

    @GetMapping("/template")
    public String template() {
        return traceTemplate.execute("demo.template", span -> {
            span.tag("biz.type", "template-test");
            return "template success";
        });
    }

    @GetMapping("/error")
    @Trace("demo.error")
    public String error() {
        throw new RuntimeException("mock error");
    }
}
