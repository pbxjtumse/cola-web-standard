package com.xjtu.iron.governance.core.engine;

import com.xjtu.iron.governance.api.context.GovernanceContext;
import com.xjtu.iron.governance.core.invocation.GovernanceInvocation;
import com.xjtu.iron.governance.core.policy.GovernancePolicy;

public interface GovernanceEngine {
    <T> T execute(GovernanceContext context, GovernancePolicy policy, GovernanceInvocation<T> invocation) throws Exception;
}