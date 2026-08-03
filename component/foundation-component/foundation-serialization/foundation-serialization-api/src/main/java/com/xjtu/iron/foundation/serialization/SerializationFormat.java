package com.xjtu.iron.foundation.serialization;

/**
 * 定义序列化数据格式。
 */
public enum SerializationFormat {
    JSON("application/json"),
    TEXT("text/plain"),
    BINARY("application/octet-stream");

    /** 该格式对应的标准 Content-Type。 */
    private final String contentType;

    SerializationFormat(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }
}
