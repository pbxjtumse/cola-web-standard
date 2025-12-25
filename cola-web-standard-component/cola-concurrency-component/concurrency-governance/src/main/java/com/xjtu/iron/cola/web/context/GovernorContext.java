package com.xjtu.iron.cola.web.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 支持：
 *  单 tag 并发限制
 *  多 tag 组合限制（AND 语义）
 *  动态调整并发阈值
 *  可扩展到 tenant / api / priority
 *  与 Executor 解耦
 * @author pangbo
 * @date 2025/12/25
 */
public final class GovernorContext {

    /**
     * tag 是一组维度键值对：
     */
    private final Map<String, String> tags;

    /**
     * @param tags
     */
    public GovernorContext(Map<String, String> tags) {
        this.tags = new HashMap<>(tags);
    }

    /**
     * @return {@link Map }<{@link String }, {@link String }>
     */
    public Map<String, String> tags() {
        return tags;
    }

    /**
     * @param key
     * @return {@link String }
     */
    public String tag(String key) {
        return tags.get(key);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, String> tags = new HashMap<>();

        public Builder api(String api) {
            tags.put("api", api);
            return this;
        }

        public Builder tenant(String tenant) {
            tags.put("tenant", tenant);
            return this;
        }

        public Builder biz(String biz) {
            tags.put("biz", biz);
            return this;
        }

        public Builder tag(String tag) {
            tags.put("tag", tag);
            return this;
        }

        public Builder put(String key, String value) {
            tags.put(key, value);
            return this;
        }

        public GovernorContext build() {
            return new GovernorContext(Collections.unmodifiableMap(tags));
        }
    }
}

