package com.xjtu.iron.cache.core.trace;

/**
 * 缓存组件 trace 上下文抽象。
 *
 * <p>用于从当前请求上下文中获取 traceId / spanId。</p>
 *
 * <p>core 层不直接依赖具体可观测实现。</p>
 */
public interface CacheTraceContext {

    /**
     * 获取当前 traceId。
     *
     * @return traceId，不存在时返回 null
     */
    String currentTraceId();

    /**
     * 获取当前 spanId。
     *
     * @return spanId，不存在时返回 null
     */
    String currentSpanId();
}