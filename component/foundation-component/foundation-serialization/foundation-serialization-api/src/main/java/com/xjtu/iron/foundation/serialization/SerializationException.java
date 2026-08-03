package com.xjtu.iron.foundation.serialization;

/**
 * 表示序列化技术能力执行失败。
 *
 * <p>该异常不声明是否可重试；消息、缓存等上层组件需要结合操作语义自行判断。</p>
 */
public class SerializationException extends RuntimeException {

    /** 发生异常的序列化操作阶段。 */
    private final SerializationOperation operation;
    /** 序列化源类型或反序列化目标类型。 */
    private final String targetType;
    /** 异常发生时已知的载荷字节长度。 */
    private final int contentLength;

    public SerializationException(String message,
                                  SerializationOperation operation,
                                  String targetType,
                                  int contentLength,
                                  Throwable cause) {
        super(message, cause);
        this.operation = operation;
        this.targetType = targetType;
        this.contentLength = contentLength;
    }

    public SerializationOperation getOperation() { return operation; }
    public String getTargetType() { return targetType; }
    public int getContentLength() { return contentLength; }
}
