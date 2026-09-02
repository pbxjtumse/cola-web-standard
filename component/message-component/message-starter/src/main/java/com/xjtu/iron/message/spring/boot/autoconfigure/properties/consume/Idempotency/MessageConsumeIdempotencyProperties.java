package com.xjtu.iron.message.spring.boot.autoconfigure.properties.consume.idempotency;

import com.xjtu.iron.message.api.consume.definition.MessageIdempotencyFailurePolicy;
import com.xjtu.iron.message.api.consume.definition.MessageIdempotencyMode;

import java.time.Duration;

/**
 * 消费幂等配置。
 *
 * <p>该类对应 {@code xjtu.iron.message.consume.idempotency.*}，
 * 不单独声明 {@code @ConfigurationProperties}。</p>
 */
public final class MessageConsumeIdempotencyProperties {

    /** 是否全局要求消费幂等能力。 */
    private boolean enabled;

    /** 默认幂等键模式。 */
    private MessageIdempotencyMode mode = MessageIdempotencyMode.MESSAGE_ID;

    /** 默认幂等命名空间。 */
    private String namespace = "message-consume";

    /** 默认幂等场景。为空时由 ConsumerDefinition 或消息信息推导。 */
    private String scene;

    /** 自定义幂等键解析器 Bean 名称。 */
    private String keyResolverBean;

    /** PROCESSING 状态超时时间。 */
    private Duration processingTimeout = Duration.ofMinutes(5);

    /** 幂等记录保留时间。 */
    private Duration recordRetention = Duration.ofDays(7);

    /** 单条消息最大处理尝试次数。 */
    private int maxAttempts = 3;

    /** 幂等失败后的默认处理策略。 */
    private MessageIdempotencyFailurePolicy failurePolicy = MessageIdempotencyFailurePolicy.RETRY;

    /** 幂等存储名称。 */
    private String storeName = "default";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public MessageIdempotencyMode getMode() {
        return mode;
    }

    public void setMode(MessageIdempotencyMode mode) {
        this.mode = mode == null ? MessageIdempotencyMode.MESSAGE_ID : mode;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public String getKeyResolverBean() {
        return keyResolverBean;
    }

    public void setKeyResolverBean(String keyResolverBean) {
        this.keyResolverBean = keyResolverBean;
    }

    public Duration getProcessingTimeout() {
        return processingTimeout;
    }

    public void setProcessingTimeout(Duration processingTimeout) {
        this.processingTimeout = processingTimeout == null ? Duration.ofMinutes(5) : processingTimeout;
    }

    public Duration getRecordRetention() {
        return recordRetention;
    }

    public void setRecordRetention(Duration recordRetention) {
        this.recordRetention = recordRetention == null ? Duration.ofDays(7) : recordRetention;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public MessageIdempotencyFailurePolicy getFailurePolicy() {
        return failurePolicy;
    }

    public void setFailurePolicy(MessageIdempotencyFailurePolicy failurePolicy) {
        this.failurePolicy = failurePolicy == null ? MessageIdempotencyFailurePolicy.RETRY : failurePolicy;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }
}
