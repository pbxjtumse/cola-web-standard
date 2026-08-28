package com.xjtu.iron.message.spring.boot.autoconfigure.properties;

import com.xjtu.iron.message.api.consume.definition.MessageIdempotencyFailurePolicy;
import com.xjtu.iron.message.api.consume.definition.MessageIdempotencyMode;

import java.time.Duration;

/** 消费幂等配置属性。 */
public final class MessageConsumeIdempotencyProperties {
    private boolean enabled;
    private MessageIdempotencyMode mode = MessageIdempotencyMode.MESSAGE_ID;
    private String namespace = "message-consume";
    private String scene;
    private String keyResolverBean;
    private Duration processingTimeout = Duration.ofMinutes(5);
    private Duration recordRetention = Duration.ofDays(7);
    private int maxAttempts = 3;
    private MessageIdempotencyFailurePolicy failurePolicy = MessageIdempotencyFailurePolicy.RETRY;
    private String storeName = "default";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public MessageIdempotencyMode getMode() { return mode; }
    public void setMode(MessageIdempotencyMode mode) { this.mode = mode; }
    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }
    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public String getKeyResolverBean() { return keyResolverBean; }
    public void setKeyResolverBean(String keyResolverBean) { this.keyResolverBean = keyResolverBean; }
    public Duration getProcessingTimeout() { return processingTimeout; }
    public void setProcessingTimeout(Duration processingTimeout) { this.processingTimeout = processingTimeout; }
    public Duration getRecordRetention() { return recordRetention; }
    public void setRecordRetention(Duration recordRetention) { this.recordRetention = recordRetention; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public MessageIdempotencyFailurePolicy getFailurePolicy() { return failurePolicy; }
    public void setFailurePolicy(MessageIdempotencyFailurePolicy failurePolicy) { this.failurePolicy = failurePolicy; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
}
