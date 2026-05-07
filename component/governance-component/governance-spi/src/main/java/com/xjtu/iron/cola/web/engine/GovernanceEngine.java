package com.xjtu.iron.cola.web.engine;



import com.xjtu.iron.cola.web.context.GovernanceContext;
import com.xjtu.iron.cola.web.invocation.GovernanceInvocation;
import com.xjtu.iron.cola.web.model.engine.GovernanceEngineCapability;
import com.xjtu.iron.cola.web.model.engine.GovernanceEngineType;
import com.xjtu.iron.cola.web.model.policy.GovernancePolicy;

import java.util.Set;

public interface GovernanceEngine {

    GovernanceEngineType engineType();

    Set<GovernanceEngineCapability> capabilities();

    <T> T execute(GovernanceContext context, GovernancePolicy policy, GovernanceInvocation<T> invocation) throws Throwable;
}
