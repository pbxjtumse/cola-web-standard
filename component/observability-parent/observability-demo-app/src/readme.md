1. HTTP 请求进入 DemoController.error()

2. 方法上有 @Trace("demo.error")

3. Spring AOP 进入 TraceAspect.around()

4. TraceAspect 调用：
   traceService.startSpan("demo.error")

5. OtelTraceServiceImpl 创建 OpenTelemetry Span

6. OtelTraceServiceImpl 把 Span 设置为 current

7. OtelTraceSpan 持有：
    - 原生 Span
    - Scope
    - errorResolvers 列表

8. TraceAspect 调用业务方法：
   joinPoint.proceed()

9. 业务方法抛出：
   DemoBizException("DEMO_BIZ_ERROR", "...")

10. TraceAspect catch 到异常

11. TraceAspect 调用：
    span.error(throwable)

12. OtelTraceSpan.error() 执行：
    - span.recordException()
    - span.setStatus(ERROR)
    - tag exception.class
    - tag exception.message
    - applyErrorResolvers()

13. applyErrorResolvers 遍历：
    DemoBizExceptionTraceErrorResolver.supports(e) == true

14. 调用：
    DemoBizExceptionTraceErrorResolver.resolve(e, span)

15. 写入业务异常标签：
    error.type = BUSINESS
    error.code = DEMO_BIZ_ERROR
    error.biz = true

16. TraceAspect 重新 throw throwable
    不吞异常

17. finally：
    span.close()
    mdcScope.close()

18. Spring MVC 返回 500