package com.xjtu.iron.foundation.serialization;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;

/**
 * 表示可跨消息、缓存或 Outbox 边界传输的序列化载荷。
 */
public final class SerializedPayload {

    /** 序列化后的防御性字节副本。 */
    private final byte[] content;
    /** 载荷使用的数据格式。 */
    private final SerializationFormat format;
    /** 序列化对象的类型名称。 */
    private final String typeName;
    /** 载荷协议或 Schema 版本。 */
    private final String schemaVersion;
    /** 文本载荷使用的字符集。 */
    private final Charset charset;

    public SerializedPayload(byte[] content,
                             SerializationFormat format,
                             String typeName,
                             String schemaVersion,
                             Charset charset) {
        this.content = Arrays.copyOf(Objects.requireNonNull(content, "content must not be null"), content.length);
        this.format = Objects.requireNonNull(format, "format must not be null");
        this.typeName = typeName;
        this.schemaVersion = schemaVersion;
        this.charset = Objects.requireNonNull(charset, "charset must not be null");
    }

    public byte[] copyContent() { return Arrays.copyOf(content, content.length); }
    public int contentLength() { return content.length; }
    public SerializationFormat getFormat() { return format; }
    public String getContentType() { return format.getContentType(); }
    public String getTypeName() { return typeName; }
    public String getSchemaVersion() { return schemaVersion; }
    public Charset getCharset() { return charset; }
}
