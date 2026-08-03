package com.xjtu.iron.foundation.id;

import com.xjtu.iron.foundation.time.ClockProvider;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Objects;

/**
 * 生成前缀按时间排序的字符串标识。
 *
 * <p>该实现不是 Snowflake，也不承诺连续性；它使用毫秒时间戳和随机尾部，
 * 适合事件、消息和执行记录等需要大致时间有序的技术标识。</p>
 */
public final class TimeSortableIdGenerator implements StringIdGenerator {

    /** 排除易混淆字符的 Crockford Base32 字符表。 */
    private static final char[] BASE32 = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    /** 提供时间排序前缀的可替换时钟。 */
    private final ClockProvider clockProvider;
    /** 生成随机尾部的安全随机源。 */
    private final SecureRandom random;

    public TimeSortableIdGenerator(ClockProvider clockProvider) {
        this(clockProvider, new SecureRandom());
    }

    public TimeSortableIdGenerator(ClockProvider clockProvider, SecureRandom random) {
        this.clockProvider = Objects.requireNonNull(clockProvider, "clockProvider must not be null");
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    @Override
    public String nextId() {
        long timestamp = Instant.from(clockProvider.now()).toEpochMilli();
        char[] result = new char[26];

        // 前十位保存 48 位毫秒时间戳，使字符串按生成时间自然排序。
        encodeTimestamp(timestamp, result);
        for (int index = 10; index < result.length; index++) {
            result[index] = BASE32[random.nextInt(BASE32.length)];
        }
        return new String(result);
    }

    private void encodeTimestamp(long timestamp, char[] result) {
        long value = timestamp;
        for (int index = 9; index >= 0; index--) {
            result[index] = BASE32[(int) (value & 31)];
            value >>>= 5;
        }
    }
}
