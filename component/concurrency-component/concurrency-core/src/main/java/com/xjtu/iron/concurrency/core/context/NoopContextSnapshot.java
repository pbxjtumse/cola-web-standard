package com.xjtu.iron.concurrency.core.context;

import com.xjtu.iron.concurrency.api.context.ContextScope;
import com.xjtu.iron.concurrency.api.context.ContextSnapshot;

public class NoopContextSnapshot implements ContextSnapshot {

    public static final NoopContextSnapshot INSTANCE = new NoopContextSnapshot();

    private NoopContextSnapshot() {
    }

    @Override
    public ContextScope restore() {
        return NoopContextScope.INSTANCE;
    }
}
