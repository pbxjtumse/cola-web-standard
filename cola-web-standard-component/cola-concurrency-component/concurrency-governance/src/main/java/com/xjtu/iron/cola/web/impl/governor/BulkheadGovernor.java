package com.xjtu.iron.cola.web.impl.governor;

import com.xjtu.iron.cola.web.Bulkhead;
import com.xjtu.iron.cola.web.impl.bulk.BulkheadRegistry;
import com.xjtu.iron.cola.web.ConcurrencyGovernor;
import com.xjtu.iron.cola.web.dto.Permit;
import com.xjtu.iron.cola.web.context.GovernorContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 同时干活的人数
 * @author pangbo
 * @date 2025/12/19
 */
@Component
public class BulkheadGovernor implements ConcurrencyGovernor {

    private final BulkheadRegistry registry;

    public BulkheadGovernor(BulkheadRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Permit tryAcquire(GovernorContext context) {
        List<Bulkhead> bulkheads = registry.match(context);
        // 最严格优先
        Collections.sort(bulkheads, Comparator.comparingInt(Bulkhead::getLimit));
        final List<Bulkhead> acquired = new ArrayList<Bulkhead>();

        for (Bulkhead bulkhead : bulkheads) {
            if (!bulkhead.tryAcquire()) {
                // 回滚
                for (Bulkhead b : acquired) {
                    b.release();
                }
                return Permit.rejected();
            }
            acquired.add(bulkhead);
        }

        // 把 release 行为转成 Runnable
        List<Runnable> releases = new ArrayList<>();
        for (final Bulkhead b : acquired) {
            releases.add(() -> b.release());
        }

        return Permit.acquired(releases);
    }
}


