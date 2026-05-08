package com.xjtu.iron.governance.api.fallback;

import com.xjtu.iron.governance.api.context.GovernanceContext;

@FunctionalInterface
public interface FallbackHandler<T> {

    T fallback(GovernanceContext context, Throwable throwable);
}
