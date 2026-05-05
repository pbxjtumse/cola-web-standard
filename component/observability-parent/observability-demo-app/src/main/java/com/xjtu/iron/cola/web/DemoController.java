package com.xjtu.iron.cola.web;

import com.xjtu.iron.cola.web.tracing.Trace;
import com.xjtu.iron.cola.web.tracing.TraceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 可观测 demo 接口。
 */
@RestController
public class DemoController {

    private static final Logger log = LoggerFactory.getLogger(DemoController.class);

    private final TraceTemplate traceTemplate;

    public DemoController(TraceTemplate traceTemplate) {
        this.traceTemplate = traceTemplate;
    }

    @GetMapping("/hello")
    @Trace("demo.hello")
    public String hello() {
        log.info("hello api called");
        return "hello observability";
    }

    @GetMapping("/template")
    @Trace("demo.template.api")
    public String template() {
        log.info("template api called");

        return traceTemplate.execute("demo.template.inner", span -> {
            span.tag("biz.type", "template-test");
            log.info("inside trace template");
            return "template success";
        });
    }

    @GetMapping("/biz-error")
    @Trace("demo.biz-error")
    public String bizerror() {
        log.info("biz-error api called");
        throw new DemoBizException("DEMO_BIZ_ERROR", "this is a demo business exception");
    }

    @GetMapping("/system-error")
    @Trace("demo.system.error")
    public String systemError() {
        log.info("system error api called");
        throw new RuntimeException("mock system error");
    }
}