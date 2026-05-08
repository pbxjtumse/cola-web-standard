package com.xjtu.iron.governance.core.resolver;

import com.xjtu.iron.governance.api.context.GovernanceContext;
import com.xjtu.iron.governance.model.policy.GovernancePolicy;

public interface EffectiveGovernancePolicyResolver {

    GovernancePolicy resolve(GovernanceContext context);
}
