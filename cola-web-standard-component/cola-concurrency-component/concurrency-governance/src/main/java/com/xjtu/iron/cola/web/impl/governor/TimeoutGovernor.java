package com.xjtu.iron.cola.web.impl.governor;

import com.xjtu.iron.cola.web.ConcurrencyGovernor;
import com.xjtu.iron.cola.web.dto.Permit;
import com.xjtu.iron.cola.web.context.GovernorContext;

import java.util.Collections;

/**
 * 单个活最多干多久
 * @author pbxjt
 * @date 2025/12/19
 */
public class TimeoutGovernor implements ConcurrencyGovernor {

    /**
     * @param context
     * @return {@link Permit }
     */
    @Override
    public Permit tryAcquire(GovernorContext context) {
        // Timeout 不在 acquire 阶段拒绝
        return Permit.acquired(Collections.<Runnable>emptyList());
    }

}

