package com.xjtu.iron.governor;

import com.xjtu.iron.ConcurrencyGovernor;
import com.xjtu.iron.dto.Permit;
import com.xjtu.iron.context.GovernorContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 单位时间内来多少活
 * @author pbxjt
 * @date 2025/12/19
 */
@Component
@Order(10)
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
