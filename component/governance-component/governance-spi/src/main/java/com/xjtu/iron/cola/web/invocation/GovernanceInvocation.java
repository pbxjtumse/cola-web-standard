package com.xjtu.iron.cola.web.invocation;


import com.xjtu.iron.cola.web.context.GovernanceContext;

@FunctionalInterface
public interface GovernanceInvocation<T> {

    T invoke(GovernanceContext context) throws Throwable;
}
