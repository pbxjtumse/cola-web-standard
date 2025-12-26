package com.xjtu.iron.cola.web.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * GovernorContext 是一次请求在“治理视角”下的事实描述，不是业务对象。
 * @author pbxjt
 * @date 2025/12/26
 */
public class GovernorContext {

    /**
     * 治理不能依赖 Order / User / DTO 、但治理又必须“感知业务差异、tags 作为事实载体。
     */
    private final Map<String, String> tags;

    /**
     * @param tags
     */
    private GovernorContext(Map<String, String> tags) {
        this.tags = tags;
    }

    /**
     * @return {@link Builder }
     */
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
        private final Map<String, String> tags = new HashMap<>();

        /**
         * @param api
         * @return {@link Builder }
         */
        public Builder api(String api) {
            tags.put("api", api);
            return this;
        }

        /**
         * @param tenant
         * @return {@link Builder }
         */
        public Builder tenant(String tenant) {
            tags.put("tenant", tenant);
            return this;
        }

        /**
         * @param biz
         * @return {@link Builder }
         */
        public Builder biz(String biz) {
            tags.put("biz", biz);
            return this;
        }

        /**
         * @param tag
         * @return {@link Builder }
         */
        public Builder tag(String tag) {
            tags.put("tag", tag);
            return this;
        }

        /**
         * @param key
         * @param value
         * @return {@link Builder }
         */
        public Builder put(String key, String value) {
            tags.put(key, value);
            return this;
        }

        /**
         * @return {@link GovernorContext }
         */
        public GovernorContext build() {
            return new GovernorContext(Collections.unmodifiableMap(tags));
        }
    }

    /**
     * @return {@link Map }<{@link String }, {@link String }>
     */
    public Map<String, String> tags() {
        return tags;
    }
}


