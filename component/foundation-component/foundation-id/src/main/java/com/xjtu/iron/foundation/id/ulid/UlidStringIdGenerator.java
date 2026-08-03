package com.xjtu.iron.foundation.id.ulid;

import com.xjtu.iron.foundation.id.api.IdGenerationException;
import com.xjtu.iron.foundation.id.api.StringIdGenerator;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Arrays;
import java.util.Objects;

/**
 * 生成 26 位、Crockford Base32 编码的单调 ULID。
 *
 * <p>标识由 48 位毫秒时间戳和 80 位随机数组成。同一毫秒内递增随机部分，保持单进程字典序单调。</p>
 */
public final class UlidStringIdGenerator implements StringIdGenerator {

    private static final long MAX_TIMESTAMP = 0x0000FFFFFFFFFFFFL;
    private static final char[] ALPHABET =
            "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    private final Clock clock;
    private final SecureRandom secureRandom;
    private final byte[] randomness = new byte[10];

    private long lastTimestamp = -1L;

    public UlidStringIdGenerator() {
        this(Clock.systemUTC(), new SecureRandom());
    }

    public UlidStringIdGenerator(Clock clock, SecureRandom secureRandom) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.secureRandom = Objects.requireNonNull(
                secureRandom,
                "secureRandom must not be null"
        );
    }

    @Override
    public synchronized String nextId() {
        long timestamp = clock.millis();
        if (timestamp < 0L) {
            throw new IdGenerationException("ULID timestamp must not be negative");
        }
        if (timestamp > MAX_TIMESTAMP) {
            throw new IdGenerationException("ULID timestamp exceeds 48-bit range");
        }

        if (timestamp > lastTimestamp) {
            secureRandom.nextBytes(randomness);
        } else {
            timestamp = lastTimestamp;
            if (!incrementRandomness()) {
                timestamp = lastTimestamp + 1L;
                Arrays.fill(randomness, (byte) 0);
            }
        }

        if (timestamp > MAX_TIMESTAMP) {
            throw new IdGenerationException("ULID timestamp space is exhausted");
        }

        lastTimestamp = timestamp;
        byte[] bytes = new byte[16];
        for (int index = 5; index >= 0; index--) {
            bytes[index] = (byte) timestamp;
            timestamp >>>= 8;
        }
        System.arraycopy(randomness, 0, bytes, 6, randomness.length);
        return encode(bytes);
    }

    private boolean incrementRandomness() {
        for (int index = randomness.length - 1; index >= 0; index--) {
            int value = (randomness[index] & 0xFF) + 1;
            randomness[index] = (byte) value;
            if (value <= 0xFF) {
                return true;
            }
        }
        return false;
    }

    private String encode(byte[] bytes) {
        BigInteger value = new BigInteger(1, bytes);
        char[] encoded = new char[26];
        BigInteger mask = BigInteger.valueOf(31L);
        for (int index = encoded.length - 1; index >= 0; index--) {
            encoded[index] = ALPHABET[value.and(mask).intValue()];
            value = value.shiftRight(5);
        }
        return new String(encoded);
    }
}
