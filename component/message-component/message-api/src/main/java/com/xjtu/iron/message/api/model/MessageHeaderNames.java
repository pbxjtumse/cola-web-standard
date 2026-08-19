package com.xjtu.iron.message.api.model;

/** 定义组件保留的线级系统消息头名称。 */
public final class MessageHeaderNames {

    public static final String SYSTEM_PREFIX = "x-iron-message-";
    public static final String MESSAGE_ID = SYSTEM_PREFIX + "id";
    public static final String MESSAGE_TYPE = SYSTEM_PREFIX + "type";
    public static final String SCHEMA_VERSION = SYSTEM_PREFIX + "schema-version";
    public static final String SOURCE = SYSTEM_PREFIX + "source";
    public static final String CORRELATION_ID = SYSTEM_PREFIX + "correlation-id";
    public static final String CAUSATION_ID = SYSTEM_PREFIX + "causation-id";
    public static final String TENANT_ID = SYSTEM_PREFIX + "tenant-id";
    public static final String OCCURRED_AT = SYSTEM_PREFIX + "occurred-at";
    public static final String CREATED_AT = SYSTEM_PREFIX + "created-at";
    public static final String CONTENT_TYPE = SYSTEM_PREFIX + "content-type";
    public static final String DESTINATION_NAMESPACE = SYSTEM_PREFIX + "destination-namespace";
    public static final String DESTINATION_NAME = SYSTEM_PREFIX + "destination-name";

    private MessageHeaderNames() {
    }

    /** 判断名称是否属于组件保留系统头。 */
    public static boolean isReserved(String name) {
        return name != null && name.startsWith(SYSTEM_PREFIX);
    }
}
