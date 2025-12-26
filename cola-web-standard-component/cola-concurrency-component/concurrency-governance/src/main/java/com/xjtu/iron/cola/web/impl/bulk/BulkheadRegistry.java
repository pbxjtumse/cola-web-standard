package com.xjtu.iron.cola.web.impl.bulk;
import com.xjtu.iron.cola.web.Bulkhead;
import com.xjtu.iron.cola.web.dto.TagRule;
import com.xjtu.iron.cola.web.context.GovernorContext;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 规则 → 资源 的映射表
 * 1.保存所有治理规则 entries、2。根据 context 找出所有命中的 bulkhead
 * @author pbxjt
 * @date 2025/12/26
 */
public class BulkheadRegistry {
    /**
     *
     */
    private final List<BulkheadEntry> entries = new CopyOnWriteArrayList<>();

    /**
     * @param rule
     * @param bulkhead
     */
    public void register(TagRule rule, Bulkhead bulkhead) {
        entries.add(new BulkheadEntry(rule, bulkhead));
    }

    /**
     * @param ctx
     * @return {@link List }<{@link Bulkhead }>
     */
    public List<Bulkhead> match(GovernorContext ctx) {
        return entries.stream()
                .filter(e -> e.rule.matches(ctx))
                .map(e -> e.bulkhead)
                .collect(Collectors.toList());
    }

    /**
     * @author pbxjt
     * @date 2025/12/26
     */
    final class BulkheadEntry {

        /**
         *
         */
        private final TagRule rule;
        /**
         *
         */
        private final Bulkhead bulkhead;

        /**
         * @param rule
         * @param bulkhead
         */
        BulkheadEntry(TagRule rule, Bulkhead bulkhead) {
            this.rule = rule;
            this.bulkhead = bulkhead;
        }

        /**
         * @return {@link TagRule }
         */
        TagRule getRule() {
            return rule;
        }

        /**
         * @return {@link Bulkhead }
         */
        Bulkhead getBulkhead() {
            return bulkhead;
        }
    }



}
