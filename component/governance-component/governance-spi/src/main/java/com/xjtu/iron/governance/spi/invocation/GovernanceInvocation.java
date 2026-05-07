package com.xjtu.iron.governance.spi.invocation;


import com.xjtu.iron.governance.api.context.GovernanceContext;

@FunctionalInterface
public interface GovernanceInvocation<T> {

    T invoke(GovernanceContext context) throws Throwable;
}
