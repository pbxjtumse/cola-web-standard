package com.xjtu.iron.concurrency.core.context;

import com.xjtu.iron.concurrency.api.context.ContextPropagator;
import com.xjtu.iron.concurrency.api.context.ContextSnapshot;

public class NoopContextPropagator implements ContextPropagator {

    @Override
    public ContextSnapshot capture() {
        return NoopContextSnapshot.INSTANCE;
    }
}
