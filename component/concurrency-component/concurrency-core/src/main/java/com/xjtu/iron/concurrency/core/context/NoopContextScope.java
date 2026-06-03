package com.xjtu.iron.concurrency.core.context;

import com.xjtu.iron.concurrency.api.context.ContextScope;

public class NoopContextScope implements ContextScope {

    public static final NoopContextScope INSTANCE = new NoopContextScope();

    private NoopContextScope() {
    }

    @Override
    public void close() {
        // no operation
    }
}
