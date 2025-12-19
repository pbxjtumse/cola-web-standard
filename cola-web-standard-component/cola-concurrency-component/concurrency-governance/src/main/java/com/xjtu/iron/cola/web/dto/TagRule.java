package com.xjtu.iron.cola.web.dto;

import com.xjtu.iron.cola.web.context.GovernorContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author pbxjtu
 */
public class TagRule {
    private final Map<String, String> matchers;

    public TagRule(Map<String, String> matchers) {
        this.matchers = new HashMap<>(matchers);
    }

    public boolean matches(GovernorContext ctx) {
        return matchers.entrySet().stream()
                .allMatch(entry -> Objects.equals(entry.getValue(), ctx.tag(entry.getKey())));
    }
}
