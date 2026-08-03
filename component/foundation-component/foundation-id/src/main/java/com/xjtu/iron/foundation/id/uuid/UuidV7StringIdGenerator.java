package com.xjtu.iron.foundation.id.uuid;

import com.xjtu.iron.foundation.id.api.IdGenerationException;
import com.xjtu.iron.foundation.id.api.StringIdGenerator;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/**
 * 生成 RFC 9562 UUID v7。
 *
 * <p>同一进程内使用逻辑时间和 74 位单调随机序列保证生成顺序。进程重启后仍依赖随机空间保证唯一性，
 * 不承诺跨进程的严格全局单调。</p>
 */
public final class UuidV7StringIdGenerator implements StringIdGenerator {

    private static final long MAX_TIMESTAMP = 0x0000FFFFFFFFFFFFL;
    private static final int MAX_RANDOM_A = 0x0FFF;
    private static final long MAX_RANDOM_B = 0x3FFFFFFFFFFFFFFFL;

    private final Clock clock;
    private final SecureRandom secureRandom;

    private long lastTimestamp = -1L;
    private int randomA;
    private long randomB;

    public UuidV7StringIdGenerator() {
        this(Clock.systemUTC(), new SecureRandom());
    }

    public UuidV7StringIdGenerator(Clock clock, SecureRandom secureRandom) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.secureRandom = Objects.requireNonNull(
                secureRandom,
                "secureRandom must not be null"
        );
    }

    @Override
    public synchronized String nextId() {
        return nextUuid().toString();
    }

    /**
     * 生成 UUID 对象，便于数据库或协议直接使用 128 位值。
     *
     * @return UUID v7
     */
    public synchronized UUID nextUuid() {
        long timestamp = clock.millis();
        if (timestamp < 0L) {
            throw new IdGenerationException("UUID v7 timestamp must not be negative");
        }
        if (timestamp > MAX_TIMESTAMP) {
            throw new IdGenerationException("UUID v7 timestamp exceeds 48-bit range");
        }

        if (timestamp > lastTimestamp) {
            seedRandomState();
        } else {
            timestamp = lastTimestamp;
            incrementRandomState();
            if (randomA == 0 && randomB == 0) {
                timestamp = lastTimestamp + 1L;
            }
        }

        if (timestamp > MAX_TIMESTAMP) {
            throw new IdGenerationException("UUID v7 timestamp space is exhausted");
        }

        lastTimestamp = timestamp;
        long mostSignificantBits = (timestamp << 16)
                | 0x7000L
                | (randomA & MAX_RANDOM_A);
        long leastSignificantBits = 0x8000000000000000L
                | (randomB & MAX_RANDOM_B);
        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    private void seedRandomState() {
        randomA = secureRandom.nextInt(MAX_RANDOM_A + 1);
        randomB = secureRandom.nextLong() & MAX_RANDOM_B;
    }

    private void incrementRandomState() {
        if (randomB < MAX_RANDOM_B) {
            randomB++;
            return;
        }

        randomB = 0L;
        if (randomA < MAX_RANDOM_A) {
            randomA++;
            return;
        }

        randomA = 0;
    }
}
