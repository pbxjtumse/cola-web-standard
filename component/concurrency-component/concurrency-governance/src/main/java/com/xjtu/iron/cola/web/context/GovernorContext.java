package com.xjtu.iron.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 1.GovernorContext 是一次请求在“治理视角”下的事实描述，不是业务对象。注意不能混入执行路由信息
 *    这里的标签是维度标签、技术表现为键值型标签 ，而不是分组标签 例如分组order 、
 * 2.维度标签 👉 用于 规则匹配 / 治理决策  这类标签：
 *      ✔ 有语义
 *      ✔ 有层级
 *      ✔ 有值
 *      ✔ 可组合
 *      ✔ 可精确匹配
 * 3. 分组标签  👉 用于 线程池选择 / executor 路由  这类标签：这是 ExecutorSelector / ThreadPoolRegistry 该关心的。
 *      ✔ 只是“归类”
 *      ✔ 没有值
 *      ✔ 不参与治理规则匹配
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
     * 如下使用建造者模式规定能写入的标签，不能自己随便写入map key value
     * @author pbxjt
     * @date 2025/12/26
     */
    public static class Builder {
        /**
         *
         */
        public final Map<String, String> tags = new HashMap<>();

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


