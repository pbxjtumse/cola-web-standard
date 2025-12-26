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
 * 「裁决者」 Governor 决定：这次请求是否能获得所有必要资源
 *
 * @author pangbo
 * @date 2025/12/19
 */
@Component
public class BulkheadGovernor implements ConcurrencyGovernor {

    /**
     *
     */
    private final BulkheadRegistry registry;

    /**
     * @param registry
     */
    public BulkheadGovernor(BulkheadRegistry registry) {
        this.registry = registry;
    }

    /**
     * @param context
     * @return {@link Permit }
     */
    @Override
    public Permit tryAcquire(GovernorContext context) {
        //关键逻辑 只是批评对应的标签规则
        List<Bulkhead> bulkheads = registry.match(context);
        // 最严格优先按照数量
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


