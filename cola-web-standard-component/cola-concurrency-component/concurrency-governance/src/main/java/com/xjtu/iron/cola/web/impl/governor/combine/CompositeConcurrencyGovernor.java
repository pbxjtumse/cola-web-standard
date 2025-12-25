package com.xjtu.iron.cola.web.impl.governor.combine;

import com.xjtu.iron.cola.web.ConcurrencyGovernor;
import com.xjtu.iron.cola.web.dto.Permit;
import com.xjtu.iron.cola.web.context.GovernorContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 把多个规则变成“AND 关系
 * @author pbxjt
 * @date 2025/12/19
 */
public class CompositeConcurrencyGovernor implements ConcurrencyGovernor {

    /**
     *
     */
    private final List<ConcurrencyGovernor> governors;

    /**
     * @param governors
     */
    public CompositeConcurrencyGovernor(List<ConcurrencyGovernor> governors) {
        this.governors = governors;
    }

    /**
     * @param context
     * @return {@link Permit }
     */
    @Override
    public Permit tryAcquire(GovernorContext context) {

        final List<Permit> acquired = new ArrayList<Permit>();

        for (ConcurrencyGovernor governor : governors) {
            //注意这里是非CompositeConcurrencyGovernor 注意！
            Permit permit = governor.tryAcquire(context);
            if (!permit.isAcquired()) {
                // 回滚
                for (Permit p : acquired) {
                    p.release();
                }
                return Permit.rejected();
            }
            acquired.add(permit);
        }

        // 合并 release 行为
        List<Runnable> releases = new ArrayList<Runnable>();
        for (final Permit p : acquired) {
            releases.add(() -> p.release());
        }

        return Permit.acquired(releases);
    }
}

