package com.xjtu.iron.cola.web.dto;

import com.xjtu.iron.cola.web.context.GovernorContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * @author pbxjtu@date 2025/12/26
 */
public class TagRule {

    /**
     *
     */
    private final Map<String, String> equals;


    /**
     * @param equals
     */
    private TagRule(Map<String, String> equals) {
        this.equals = equals;
    }

    /**
     * 这个治理规则，是判断 否适用于当前请求
     * @param context
     * @return boolean
     */
    public boolean matches(GovernorContext context) {
        Map<String, String> tags = context.tags();
        for (Map.Entry<String, String> entry : equals.entrySet()) {
            String actual = tags.get(entry.getKey());
            if (!entry.getValue().equals(actual)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return {@link Builder }
     */// ---------- DSL ----------
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @author pbxjt
     * @date 2025/12/26
     */
    public static class Builder {
        /**
         *
         */
        private final Map<String, String> equals = new HashMap<>();

        /**
         * @param api
         * @return {@link Builder }
         */
        public Builder api(String api) {
            equals.put("api", api);
            return this;
        }

        /**
         * @param tenant
         * @return {@link Builder }
         */
        public Builder tenant(String tenant) {
            equals.put("tenant", tenant);
            return this;
        }

        /**
         * @param tag
         * @return {@link Builder }
         */
        public Builder tag(String tag) {
            equals.put("tag", tag);
            return this;
        }


        /**
         * @param key
         * @param value
         * @return {@link Builder }
         */
        public Builder put(String key, String value) {
            equals.put(key, value);
            return this;
        }

        /**
         * @return {@link TagRule }
         */
        public TagRule build() {
            return new TagRule(Collections.unmodifiableMap(equals));
        }
    }
}

