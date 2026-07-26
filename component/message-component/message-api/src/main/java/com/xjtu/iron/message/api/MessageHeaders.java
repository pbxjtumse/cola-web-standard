package com.xjtu.iron.message.api;

import java.util.Map;

/**
 * 定义消息组件保留的系统消息头以及用户消息头校验规则。
 */
public final class MessageHeaders {

    /** 系统消息头统一前缀。 */
    public static final String SYSTEM_PREFIX = "x-iron-message-";

    /** 消息唯一标识。 */
    public static final String MESSAGE_ID = SYSTEM_PREFIX + "id";

    /** 业务消息类型。 */
    public static final String MESSAGE_TYPE = SYSTEM_PREFIX + "type";

    /** 消息结构版本。 */
    public static final String SCHEMA_VERSION = SYSTEM_PREFIX + "schema-version";

    /** 消息来源。 */
    public static final String SOURCE = SYSTEM_PREFIX + "source";

    /** 业务过程关联标识。 */
    public static final String CORRELATION_ID = SYSTEM_PREFIX + "correlation-id";

    /** 直接上游消息 ID。 */
    public static final String CAUSATION_ID = SYSTEM_PREFIX + "causation-id";

    /** 租户标识。 */
    public static final String TENANT_ID = SYSTEM_PREFIX + "tenant-id";

    /** 业务事件发生时间。 */
    public static final String OCCURRED_AT = SYSTEM_PREFIX + "occurred-at";

    /** 消息信封创建时间。 */
    public static final String CREATED_AT = SYSTEM_PREFIX + "created-at";

    /** 序列化后的消息体媒体类型。 */
    public static final String CONTENT_TYPE = SYSTEM_PREFIX + "content-type";

    /** 逻辑目的地命名空间。 */
    public static final String DESTINATION_NAMESPACE = SYSTEM_PREFIX + "destination-namespace";

    /** 逻辑目的地名称。 */
    public static final String DESTINATION_NAME = SYSTEM_PREFIX + "destination-name";

    /** 逻辑目的地类别。 */
    public static final String DESTINATION_CATEGORY = SYSTEM_PREFIX + "destination-category";

    /**
     * 工具类不允许创建实例。
     */
    private MessageHeaders() {
        // 私有构造器阻止无意义实例化。
    }

    /**
     * 判断指定消息头是否属于系统保留范围。
     *
     * @param headerName 消息头名称
     * @return 属于系统保留范围时返回 true
     */
    public static boolean isReserved(String headerName) {
        // 空名称不属于合法消息头，也不属于系统保留头。
        return headerName != null && headerName.startsWith(SYSTEM_PREFIX);
    }

    /**
     * 校验用户消息头，阻止业务伪造系统元数据。
     *
     * @param headers 用户消息头
     */
    public static void validateUserHeaders(Map<String, String> headers) {
        // 空映射无需校验。
        if (headers == null || headers.isEmpty()) {
            // 直接结束校验。
            return;
        }
        // 逐个复用单消息头校验，保持 Builder 和批量入口行为一致。
        headers.forEach(MessageHeaders::validateUserHeader);
    }

    /**
     * 校验单个用户消息头。
     *
     * @param name 消息头名称
     * @param value 消息头值
     */
    public static void validateUserHeader(String name, String value) {
        // 消息头名称不能为空。
        if (name == null || name.isBlank()) {
            // 使用参数异常向调用方暴露非法输入。
            throw new IllegalArgumentException("message header name must not be blank");
        }
        // 用户不能覆盖 x-iron-message-* 系统头。
        if (isReserved(name)) {
            // 明确指出冲突的消息头名称。
            throw new IllegalArgumentException("reserved message header: " + name);
        }
        // 第一版所有消息头都要求字符串值非空。
        if (value == null) {
            // Provider 对 null header 的处理差异较大，因此公共 API 直接禁止。
            throw new IllegalArgumentException("message header value must not be null: " + name);
        }
    }
}
