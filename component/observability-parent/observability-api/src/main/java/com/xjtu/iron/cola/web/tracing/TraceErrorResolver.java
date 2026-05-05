package com.xjtu.iron.cola.web.tracing;

/**
 * Trace 异常解析扩展点。
 *
 * <p>用于适配不同项目、不同公司自己的异常体系。</p>
 *
 * <p>注意：</p>
 * <ul>
 *     <li>observability 组件不定义业务异常</li>
 *     <li>observability 组件不依赖 BizException / BusinessException 等业务类</li>
 *     <li>业务项目可以自己实现该接口，把业务异常信息写入 Span Tag</li>
 * </ul>
 *
 * <p>典型用途：</p>
 * <pre>
 * error.type = BUSINESS
 * error.code = ORDER_STOCK_NOT_ENOUGH
 * error.biz = true
 * </pre>
 */
public interface TraceErrorResolver {

    /**
     * 当前 Resolver 是否支持解析该异常。
     *
     * @param throwable 当前捕获到的异常
     * @return true 表示支持解析
     */
    boolean supports(Throwable throwable);

    /**
     * 解析异常，并把可观测信息写入当前 Span。
     *
     * <p>注意：</p>
     * <ul>
     *     <li>这里只负责提取观测信息</li>
     *     <li>不要吞异常</li>
     *     <li>不要抛业务异常</li>
     *     <li>不要在这里做复杂业务逻辑</li>
     * </ul>
     *
     * @param throwable 当前捕获到的异常
     * @param span 当前 Span
     */
    void resolve(Throwable throwable, ITraceSpan span);
}