package com.xjtu.iron.cola.web;

import com.xjtu.iron.cola.web.context.GovernorContext;
import com.xjtu.iron.cola.web.dto.Permit;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GovernorChain {

    private final List<ConcurrencyGovernor> governors;

    public GovernorChain(List<ConcurrencyGovernor> governors) {
        this.governors = governors;
    }

    public Permit tryAcquire(GovernorContext context) {
        List<Permit> acquired = new ArrayList<>();

        for (ConcurrencyGovernor governor : governors) {
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

        // 合并 Permit
        List<Runnable> releaseActions = new ArrayList<>();
        for (Permit p : acquired) {
            releaseActions.add(p::release);
        }

        return Permit.acquired(releaseActions);
    }
}

