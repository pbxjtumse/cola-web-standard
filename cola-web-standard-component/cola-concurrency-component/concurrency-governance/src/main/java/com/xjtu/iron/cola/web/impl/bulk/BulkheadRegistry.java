package com.xjtu.iron.cola.web.impl.bulk;
import com.xjtu.iron.cola.web.Bulkhead;
import com.xjtu.iron.cola.web.dto.TagRule;
import com.xjtu.iron.cola.web.context.GovernorContext;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class BulkheadRegistry {
    private final List<BulkheadEntry> entries = new CopyOnWriteArrayList<>();

    public void register(TagRule rule, Bulkhead bulkhead) {
        entries.add(new BulkheadEntry(rule, bulkhead));
    }

    public List<Bulkhead> match(GovernorContext ctx) {
        return entries.stream()
                .filter(e -> e.rule.matches(ctx))
                .map(e -> e.bulkhead)
                .collect(Collectors.toList());
    }

    final class BulkheadEntry {

        private final TagRule rule;
        private final Bulkhead bulkhead;

        BulkheadEntry(TagRule rule, Bulkhead bulkhead) {
            this.rule = rule;
            this.bulkhead = bulkhead;
        }

        TagRule getRule() {
            return rule;
        }

        Bulkhead getBulkhead() {
            return bulkhead;
        }
    }



}
