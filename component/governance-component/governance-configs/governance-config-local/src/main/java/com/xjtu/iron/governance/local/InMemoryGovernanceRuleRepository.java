package com.xjtu.iron.governance.local;

import com.xjtu.iron.governance.config.api.GovernanceRuleRepository;
import com.xjtu.iron.governance.model.rule.GovernanceRuleSet;

public class InMemoryGovernanceRuleRepository implements GovernanceRuleRepository {

    private final GovernanceRuleSet ruleSet;

    public InMemoryGovernanceRuleRepository(GovernanceRuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    @Override
    public GovernanceRuleSet current() {
        return ruleSet;
    }
}
