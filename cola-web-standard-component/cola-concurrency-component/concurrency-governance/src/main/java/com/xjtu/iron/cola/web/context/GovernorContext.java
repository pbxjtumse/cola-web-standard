package com.xjtu.iron.cola.web.context;

import java.util.HashMap;
import java.util.Map;

public final class GovernorContext {

    private final Map<String, String> tags;

    public GovernorContext(Map<String, String> tags) {
        this.tags = new HashMap<>(tags);
    }

    public Map<String, String> tags() {
        return tags;
    }

    public String tag(String key) {
        return tags.get(key);
    }
}

