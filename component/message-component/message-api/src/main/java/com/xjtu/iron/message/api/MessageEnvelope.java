package com.xjtu.iron.message.api;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 表示业务提交和消费的统一不可变消息信封。
 *
 * <p>消息信封只包含跨 Provider 稳定的公共语义。Kafka partition、RocketMQ tag、
 * Pulsar subscription 等原生字段不进入该模型。</p>
 *
 * @param <T> 业务消息体类型
 */
public final class MessageEnvelope<T> {

    /** 消息唯一标识；发送前允许为空，由 core 统一生成。 */
    private final String messageId;

    /** 稳定的业务消息类型，例如 OrderPaid。 */
    private final String messageType;

    /** 消息结构版本，例如 1 或 2026-07。 */
    private final String schemaVersion;

    /** 业务消息体。 */
    private final T payload;

    /** 用于分区、局部顺序或业务定位的消息键。 */
    private final String key;

    /** 需要跨消息传播的稳定业务上下文。 */
    private final MessageContext context;

    /** 用户自定义和技术传播消息头，不包含组件保留系统头。 */
    private final Map<String, String> headers;

    /** 业务事实实际发生时间；发送前允许为空。 */
    private final Instant occurredAt;

    /** 消息信封创建时间；发送前允许为空，由 core 统一生成。 */
    private final Instant createdAt;

    /**
     * 使用 Builder 构造不可变消息信封。
     *
     * @param builder 已完成字段设置的 Builder
     */
    private MessageEnvelope(Builder<T> builder) {
        // messageType 是消息反序列化、治理和审计的核心字段，因此必须存在。
        this.messageType = requireText(builder.messageType, "messageType");
        // 业务消息体不能为空，空语义应由明确的空对象表达。
        this.payload = Objects.requireNonNull(builder.payload, "payload must not be null");
        // 发送前 messageId 允许为空，发送后由 core 保证存在。
        this.messageId = normalize(builder.messageId);
        // schemaVersion 允许业务不填写，由 core 使用默认版本补齐。
        this.schemaVersion = normalize(builder.schemaVersion);
        // key 允许为空，因为并非所有消息都要求分区键。
        this.key = normalize(builder.key);
        // 使用空对象代替 null，降低后续上下文丰富逻辑复杂度。
        this.context = builder.context == null ? MessageContext.empty() : builder.context;
        // 复制消息头，防止调用方后续修改原始 Map。
        this.headers = immutableHeaders(builder.headers);
        // occurredAt 允许为空，由 core 默认使用 createdAt。
        this.occurredAt = builder.occurredAt;
        // createdAt 允许为空，由 core 使用统一 Clock 生成。
        this.createdAt = builder.createdAt;
    }

    /**
     * 创建只包含消息类型和消息体的 Builder。
     *
     * @param messageType 业务消息类型
     * @param payload 业务消息体
     * @param <T> 消息体类型
     * @return 新 Builder
     */
    public static <T> Builder<T> builder(String messageType, T payload) {
        // 使用 Builder 避免九个构造参数导致字段顺序错误。
        return new Builder<>(messageType, payload);
    }

    /**
     * 创建最小消息信封。
     *
     * @param messageType 业务消息类型
     * @param payload 业务消息体
     * @param <T> 消息体类型
     * @return 最小消息信封
     */
    public static <T> MessageEnvelope<T> of(String messageType, T payload) {
        // 最小工厂适合不需要额外上下文和消息头的简单发送场景。
        return builder(messageType, payload).build();
    }

    /**
     * 返回复制当前全部字段的 Builder。
     *
     * @return 已复制当前字段的 Builder
     */
    public Builder<T> toBuilder() {
        // 逐个复制字段，保证后续修改不会影响当前不可变实例。
        return new Builder<>(messageType, payload)
                .messageId(messageId)
                .schemaVersion(schemaVersion)
                .key(key)
                .context(context)
                .headers(headers)
                .occurredAt(occurredAt)
                .createdAt(createdAt);
    }

    /** @return 消息唯一标识 */
    public String messageId() {
        // 返回不可变字段。
        return messageId;
    }

    /** @return 业务消息类型 */
    public String messageType() {
        // 返回不可变字段。
        return messageType;
    }

    /** @return 消息结构版本 */
    public String schemaVersion() {
        // 返回不可变字段。
        return schemaVersion;
    }

    /** @return 业务消息体 */
    public T payload() {
        // 返回业务消息体引用；业务消息体自身是否可变由调用方负责。
        return payload;
    }

    /** @return 消息键 */
    public String key() {
        // 返回不可变字段。
        return key;
    }

    /** @return 稳定业务上下文 */
    public MessageContext context() {
        // MessageContext 是不可变 record，可安全返回。
        return context;
    }

    /** @return 只读用户消息头 */
    public Map<String, String> headers() {
        // 返回构造时生成的不可变 Map。
        return headers;
    }

    /** @return 业务事件发生时间 */
    public Instant occurredAt() {
        // Instant 是不可变类型，可安全返回。
        return occurredAt;
    }

    /** @return 消息创建时间 */
    public Instant createdAt() {
        // Instant 是不可变类型，可安全返回。
        return createdAt;
    }

    /**
     * 标准化可选字符串。
     *
     * @param value 原始字符串
     * @return 去除首尾空白后的字符串；空白字符串转换为 null
     */
    private static String normalize(String value) {
        // null 直接保持 null。
        if (value == null) {
            // 返回 null 表示调用方未提供该字段。
            return null;
        }
        // 去除意外的首尾空白。
        String trimmed = value.trim();
        // 空白字符串和未提供字段使用同一语义。
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 校验必填文本。
     *
     * @param value 原始值
     * @param fieldName 字段名称
     * @return 标准化后的非空文本
     */
    private static String requireText(String value, String fieldName) {
        // 先执行通用标准化。
        String normalized = normalize(value);
        // 必填字段为空时立即失败。
        if (normalized == null) {
            // 抛出参数异常，避免非法消息进入 core。
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        // 返回经过标准化的合法文本。
        return normalized;
    }

    /**
     * 复制并校验用户消息头。
     *
     * @param headers 原始消息头
     * @return 不可变消息头
     */
    private static Map<String, String> immutableHeaders(Map<String, String> headers) {
        // null 或空 Map 统一返回 JDK 空不可变 Map。
        if (headers == null || headers.isEmpty()) {
            // 减少不必要对象分配。
            return Map.of();
        }
        // 先校验用户不能伪造系统消息头。
        MessageHeaders.validateUserHeaders(headers);
        // LinkedHashMap 保留插入顺序，便于测试和日志稳定输出。
        return Collections.unmodifiableMap(new LinkedHashMap<>(headers));
    }

    /**
     * 用于构造 MessageEnvelope 的 Builder。
     *
     * @param <T> 业务消息体类型
     */
    public static final class Builder<T> {

        /** 消息唯一标识。 */
        private String messageId;

        /** 业务消息类型。 */
        private final String messageType;

        /** 消息结构版本。 */
        private String schemaVersion;

        /** 业务消息体。 */
        private final T payload;

        /** 消息键。 */
        private String key;

        /** 稳定业务上下文。 */
        private MessageContext context;

        /** 用户消息头。 */
        private final Map<String, String> headers = new LinkedHashMap<>();

        /** 业务事件发生时间。 */
        private Instant occurredAt;

        /** 消息创建时间。 */
        private Instant createdAt;

        /**
         * 创建 Builder。
         *
         * @param messageType 业务消息类型
         * @param payload 业务消息体
         */
        private Builder(String messageType, T payload) {
            // 必填字段在最终 build 时再次统一校验。
            this.messageType = messageType;
            // 保存业务消息体。
            this.payload = payload;
        }

        /** @param messageId 消息 ID @return 当前 Builder */
        public Builder<T> messageId(String messageId) {
            // 允许业务在 Outbox 等场景预先生成稳定消息 ID。
            this.messageId = messageId;
            // 返回当前 Builder 以支持链式调用。
            return this;
        }

        /** @param schemaVersion 结构版本 @return 当前 Builder */
        public Builder<T> schemaVersion(String schemaVersion) {
            // 保存业务显式声明的结构版本。
            this.schemaVersion = schemaVersion;
            // 返回当前 Builder 以支持链式调用。
            return this;
        }

        /** @param key 消息键 @return 当前 Builder */
        public Builder<T> key(String key) {
            // key 通常用于 Kafka partition、RocketMQ key 或 Pulsar key。
            this.key = key;
            // 返回当前 Builder 以支持链式调用。
            return this;
        }

        /** @param context 稳定业务上下文 @return 当前 Builder */
        public Builder<T> context(MessageContext context) {
            // 保存显式上下文；缺失字段由 core 根据当前入站消息和配置补齐。
            this.context = context;
            // 返回当前 Builder 以支持链式调用。
            return this;
        }

        /** @param name 消息头名称 @param value 消息头值 @return 当前 Builder */
        public Builder<T> header(String name, String value) {
            // 单个消息头也执行系统保留前缀检查。
            MessageHeaders.validateUserHeader(name, value);
            // 后写入值覆盖同名旧值。
            this.headers.put(name, value);
            // 返回当前 Builder 以支持链式调用。
            return this;
        }

        /** @param additionalHeaders 需要合并的消息头 @return 当前 Builder */
        public Builder<T> headers(Map<String, String> additionalHeaders) {
            // null 表示调用方没有额外消息头。
            if (additionalHeaders == null || additionalHeaders.isEmpty()) {
                // 保持当前 Builder 不变。
                return this;
            }
            // 先统一校验新增消息头。
            MessageHeaders.validateUserHeaders(additionalHeaders);
            // 后加入值覆盖同名旧值。
            this.headers.putAll(additionalHeaders);
            // 返回当前 Builder 以支持链式调用。
            return this;
        }

        /** @param occurredAt 业务事件发生时间 @return 当前 Builder */
        public Builder<T> occurredAt(Instant occurredAt) {
            // 保存业务事实发生时间。
            this.occurredAt = occurredAt;
            // 返回当前 Builder 以支持链式调用。
            return this;
        }

        /** @param createdAt 消息创建时间 @return 当前 Builder */
        public Builder<T> createdAt(Instant createdAt) {
            // 通常由 core 生成；Outbox 恢复场景允许显式保留原始创建时间。
            this.createdAt = createdAt;
            // 返回当前 Builder 以支持链式调用。
            return this;
        }

        /**
         * 构造不可变消息信封。
         *
         * @return 不可变消息信封
         */
        public MessageEnvelope<T> build() {
            // 统一在私有构造器中执行校验和防御性复制。
            return new MessageEnvelope<>(this);
        }
    }
}
