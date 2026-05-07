package com.xjtu.iron.cola.web.fallback;

import com.xjtu.iron.cola.web.context.GovernanceContext;

@FunctionalInterface
public interface FallbackHandler<T> {

    T fallback(GovernanceContext context, Throwable throwable);
}
