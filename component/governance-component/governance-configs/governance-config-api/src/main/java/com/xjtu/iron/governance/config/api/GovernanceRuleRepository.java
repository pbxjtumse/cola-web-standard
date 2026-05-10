package com.xjtu.iron.governance.config.api;


import com.xjtu.iron.governance.model.rule.GovernanceRuleSet;

public interface GovernanceRuleRepository {

    GovernanceRuleSet current();
}
