package com.xjtu.iron.foundation.context;

import java.time.Instant;
import java.util.Objects;

/**
 * 表示某个时刻捕获的执行上下文快照。
 */
public final class ContextSnapshot {

    /** 捕获时刻对应的不可变执行上下文。 */
    private final ExecutionContext context;
    /** 上下文快照的捕获时间。 */
    private final Instant capturedAt;

    public ContextSnapshot(ExecutionContext context, Instant capturedAt) {
        this.context = Objects.requireNonNull(context, "context must not be null");
        this.capturedAt = Objects.requireNonNull(capturedAt, "capturedAt must not be null");
    }

    public ExecutionContext getContext() { return context; }
    public Instant getCapturedAt() { return capturedAt; }
}
