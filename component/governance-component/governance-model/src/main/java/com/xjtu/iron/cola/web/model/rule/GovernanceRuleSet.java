package com.xjtu.iron.cola.web.model.rule;

import com.xjtu.iron.cola.web.model.policy.GovernancePolicy;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class GovernanceRuleSet {

    private String version = "local";

    private Instant loadedAt = Instant.now();

    private GovernancePolicy defaultPolicy = new GovernancePolicy();

    private Map<String, GovernancePolicy> resources = new HashMap<>();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }


    public Instant getLoadedAt() {
        return loadedAt;
    }

    public void setLoadedAt(Instant loadedAt) {
        this.loadedAt = loadedAt;
    }

    public GovernancePolicy getDefaultPolicy() {
        return defaultPolicy;
    }

    public void setDefaultPolicy(GovernancePolicy defaultPolicy) {
        this.defaultPolicy = defaultPolicy;
    }

    public Map<String, GovernancePolicy> getResources() {
        return resources;
    }

    public void setResources(Map<String, GovernancePolicy> resources) {
        this.resources = resources;
    }
}
