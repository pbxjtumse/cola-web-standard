package com.xjtu.iron.foundation.resource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

/**
 * 基于防御性字节数组副本的内存资源。
 */
public final class ByteArrayResource implements Resource {

    /** 资源内容的防御性字节副本。 */
    private final byte[] content;
    /** 用于日志和异常信息的资源描述。 */
    private final String description;

    public ByteArrayResource(byte[] content, String description) {
        this.content = Arrays.copyOf(
                java.util.Objects.requireNonNull(content, "content must not be null"),
                content.length
        );
        this.description = description == null ? "in-memory resource" : description;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public InputStream openStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public boolean exists() {
        return true;
    }

    public byte[] copyContent() {
        return Arrays.copyOf(content, content.length);
    }
}
