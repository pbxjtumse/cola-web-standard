package com.xjtu.iron.foundation.id.snowflake;

import com.xjtu.iron.foundation.id.api.IdGenerationException;
import com.xjtu.iron.foundation.id.api.LongIdGenerator;

import java.time.Clock;
import java.util.Objects;

/**
 * 生成 64 位 Snowflake 标识。
 *
 * <p>位布局为 1 位符号位、41 位毫秒时间、10 位 workerId、12 位同毫秒序列。
 * 单节点每毫秒最多生成 4096 个标识。workerId 必须由部署系统保证唯一。</p>
 */
public final class SnowflakeLongIdGenerator implements LongIdGenerator {

    private static final long MAX_TIMESTAMP_DELTA = (1L << 41) - 1L;
    private static final long SEQUENCE_MASK = (1L << 12) - 1L;
    private static final int WORKER_ID_SHIFT = 12;
    private static final int TIMESTAMP_SHIFT = 22;

    private final SnowflakeOptions options;
    private final Clock clock;

    private long lastTimestamp = -1L;
    private long sequence;

    public SnowflakeLongIdGenerator(SnowflakeOptions options) {
        this(options, Clock.systemUTC());
    }

    public SnowflakeLongIdGenerator(SnowflakeOptions options, Clock clock) {
        this.options = Objects.requireNonNull(options, "options must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public synchronized long nextLongId() {
        long timestamp = resolveTimestamp(clock.millis());
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1L) & SEQUENCE_MASK;
            if (sequence == 0L) {
                timestamp = nextTimestampAfterSequenceOverflow(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        long timestampDelta = timestamp - options.getEpochMillis();
        if (timestampDelta < 0L) {
            throw new IdGenerationException("current time is before Snowflake epoch");
        }
        if (timestampDelta > MAX_TIMESTAMP_DELTA) {
            throw new IdGenerationException("Snowflake timestamp space is exhausted");
        }

        lastTimestamp = timestamp;
        return (timestampDelta << TIMESTAMP_SHIFT)
                | (options.getWorkerId() << WORKER_ID_SHIFT)
                | sequence;
    }

    private long resolveTimestamp(long currentTimestamp) {
        if (currentTimestamp >= lastTimestamp) {
            return currentTimestamp;
        }

        long backwardMillis = lastTimestamp - currentTimestamp;
        if (options.getClockRollbackStrategy() == ClockRollbackStrategy.FAIL_FAST
                || backwardMillis > options.getMaxBackwardMillis()) {
            throw new IdGenerationException(
                    "clock moved backwards by " + backwardMillis + " ms"
            );
        }
        return lastTimestamp;
    }

    private long nextTimestampAfterSequenceOverflow(long currentLastTimestamp) {
        long timestamp = clock.millis();
        if (timestamp <= currentLastTimestamp
                && options.getClockRollbackStrategy() == ClockRollbackStrategy.USE_LOGICAL_TIME) {
            return currentLastTimestamp + 1L;
        }

        while (timestamp <= currentLastTimestamp) {
            Thread.onSpinWait();
            timestamp = clock.millis();
        }
        return timestamp;
    }
}
