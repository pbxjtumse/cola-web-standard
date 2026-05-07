package com.xjtu.iron.cola.web;


import com.xjtu.iron.governance.model.rule.GovernanceRuleSet;

public interface GovernanceRuleRepository {

    GovernanceRuleSet current();
}
