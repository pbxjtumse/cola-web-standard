package com.xjtu.iron.message.api;

/**
 * 定义消息组件内部使用的标准消息头名称。
 *
 * <p>业务可以增加自己的消息头，但不应该覆盖以下系统消息头。</p>
 */
public final class MessageHeaders {

    /** 保存消息唯一标识的标准消息头。 */
    public static final String MESSAGE_ID = "iron-message-id";

    /** 保存消息类型的标准消息头。 */
    public static final String MESSAGE_TYPE = "iron-message-type";

    /** 保存消息来源应用的标准消息头。 */
    public static final String MESSAGE_SOURCE = "iron-message-source";

    /** 保存消息结构版本的标准消息头。 */
    public static final String SCHEMA_VERSION = "iron-schema-version";

    /** 保存消息创建时间的标准消息头。 */
    public static final String CREATED_AT = "iron-created-at";

    /**
     * 禁止外部实例化常量工具类。
     */
    private MessageHeaders() {
        // 常量类不需要实例。
    }
}
