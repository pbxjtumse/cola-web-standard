package com.xjtu.iron.foundation.id.decorator;

import com.xjtu.iron.foundation.id.api.IdGenerator;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 按顺序组合多个标识片段。
 */
public final class CompositeIdGenerator implements IdGenerator<String> {

    /** 按照顺序生成标识片段的函数集合。 */
    private final List<Supplier<String>> segments;
    /** 连接不同标识片段的分隔符。 */
    private final String delimiter;

    public CompositeIdGenerator(List<Supplier<String>> segments, String delimiter) {
        if (segments == null || segments.isEmpty()) {
            throw new IllegalArgumentException("segments must not be empty");
        }
        this.segments = List.copyOf(segments);
        this.segments.forEach(segment -> Objects.requireNonNull(segment, "segment must not be null"));
        this.delimiter = Objects.requireNonNull(delimiter, "delimiter must not be null");
    }

    @Override
    public String nextId() {
        return segments.stream().map(Supplier::get).collect(java.util.stream.Collectors.joining(delimiter));
    }
}
