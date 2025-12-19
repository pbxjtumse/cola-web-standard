package com.xjtu.iron.cola.web.impl.governor;

import com.xjtu.iron.cola.web.ConcurrencyGovernor;
import com.xjtu.iron.cola.web.dto.Permit;
import com.xjtu.iron.cola.web.context.GovernorContext;

import java.util.Collections;

/**
 * 单位时间内来多少活
 * @author pbxjt
 * @date 2025/12/19
 */
public class RateLimitGovernor implements ConcurrencyGovernor {

    /**
     * @param context
     * @return {@link Permit }
     */
    @Override
    public Permit tryAcquire(GovernorContext context) {
        // 未来：token bucket / leaky bucket
        return Permit.acquired(Collections.<Runnable>emptyList());
    }
}
