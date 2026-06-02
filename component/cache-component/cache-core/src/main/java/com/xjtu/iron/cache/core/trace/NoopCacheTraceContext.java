package com.xjtu.iron.cache.core.trace;


/**
 * 空 trace 上下文。
 *
 * <p>没有接入可观测组件时使用。</p>
 */
public class NoopCacheTraceContext implements CacheTraceContext {

    @Override
    public String currentTraceId() {
        return null;
    }

    @Override
    public String currentSpanId() {
        return null;
    }
}
