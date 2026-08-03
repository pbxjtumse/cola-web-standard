package com.xjtu.iron.foundation.id.snowflake;

import java.time.Instant;
import java.util.Objects;

/** Snowflake 位布局、节点和时钟回拨配置。 */
public final class SnowflakeOptions {

    public static final long DEFAULT_EPOCH_MILLIS =
            Instant.parse("2024-01-01T00:00:00Z").toEpochMilli();

    private final long epochMillis;
    private final long workerId;
    private final ClockRollbackStrategy clockRollbackStrategy;
    private final long maxBackwardMillis;

    private SnowflakeOptions(Builder builder) {
        if (builder.workerId < 0L || builder.workerId > 1023L) {
            throw new IllegalArgumentException("workerId must be between 0 and 1023");
        }
        if (builder.epochMillis < 0L) {
            throw new IllegalArgumentException("epochMillis must not be negative");
        }
        if (builder.maxBackwardMillis < 0L) {
            throw new IllegalArgumentException("maxBackwardMillis must not be negative");
        }
        this.epochMillis = builder.epochMillis;
        this.workerId = builder.workerId;
        this.clockRollbackStrategy = Objects.requireNonNull(
                builder.clockRollbackStrategy,
                "clockRollbackStrategy must not be null"
        );
        this.maxBackwardMillis = builder.maxBackwardMillis;
    }

    public static Builder builder(long workerId) {
        return new Builder(workerId);
    }

    public long getEpochMillis() {
        return epochMillis;
    }

    public long getWorkerId() {
        return workerId;
    }

    public ClockRollbackStrategy getClockRollbackStrategy() {
        return clockRollbackStrategy;
    }

    public long getMaxBackwardMillis() {
        return maxBackwardMillis;
    }

    public static final class Builder {

        private final long workerId;
        private long epochMillis = DEFAULT_EPOCH_MILLIS;
        private ClockRollbackStrategy clockRollbackStrategy =
                ClockRollbackStrategy.FAIL_FAST;
        private long maxBackwardMillis;

        private Builder(long workerId) {
            this.workerId = workerId;
        }

        public Builder epochMillis(long epochMillis) {
            this.epochMillis = epochMillis;
            return this;
        }

        public Builder clockRollbackStrategy(
                ClockRollbackStrategy clockRollbackStrategy) {
            this.clockRollbackStrategy = clockRollbackStrategy;
            return this;
        }

        public Builder maxBackwardMillis(long maxBackwardMillis) {
            this.maxBackwardMillis = maxBackwardMillis;
            return this;
        }

        public SnowflakeOptions build() {
            return new SnowflakeOptions(this);
        }
    }
}
