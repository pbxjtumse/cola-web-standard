package com.xjtu.iron.foundation.test.id;

import com.xjtu.iron.foundation.id.api.IdGenerator;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 按顺序生成可预测标识的测试生成器。
 */
public final class SequentialStringIdGenerator implements IdGenerator<String> {

    /** 测试序列标识前缀。 */
    private final String prefix;
    /** 线程安全的测试序号。 */
    private final AtomicLong sequence;

    public SequentialStringIdGenerator(String prefix) {
        this(prefix, 0L);
    }

    public SequentialStringIdGenerator(String prefix, long initialValue) {
        this.prefix = prefix == null ? "" : prefix;
        this.sequence = new AtomicLong(initialValue);
    }

    @Override
    public String nextId() {
        return prefix + sequence.incrementAndGet();
    }
}
