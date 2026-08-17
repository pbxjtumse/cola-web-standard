package com.xjtu.iron.idempotent.api.repository.acquire;

import com.xjtu.iron.idempotent.api.policy.IdempotencyMode;
import com.xjtu.iron.idempotent.api.recovery.IdempotencyRecoveryMode;
import com.xjtu.iron.idempotent.api.policy.IdempotencyWindowPolicy;

import java.time.Duration;
import java.time.Instant;

/** Repository 普通抢占请求。 */
public final class IdempotencyAcquireRequest {

    private final String namespace;
    private final String key;
    private final String requestHash;
    private final String routeKey;
    private final String ownerToken;
    private final IdempotencyMode mode;
    private final Duration processingTimeout;
    private final Duration idempotencyWindow;
    private final IdempotencyWindowPolicy windowPolicy;
    private final Duration recordRetentionTtl;
    private final IdempotencyRecoveryMode recoveryMode;
    private final Instant now;

    public IdempotencyAcquireRequest(
            String namespace,
            String key,
            String requestHash,
            String routeKey,
            String ownerToken,
            IdempotencyMode mode,
            Duration processingTimeout,
            Duration idempotencyWindow,
            IdempotencyWindowPolicy windowPolicy,
            Duration recordRetentionTtl,
            IdempotencyRecoveryMode recoveryMode,
            Instant now) {
        this.namespace = namespace;
        this.key = key;
        this.requestHash = requestHash;
        this.routeKey = routeKey;
        this.ownerToken = ownerToken;
        this.mode = mode;
        this.processingTimeout = processingTimeout;
        this.idempotencyWindow = idempotencyWindow;
        this.windowPolicy = windowPolicy;
        this.recordRetentionTtl = recordRetentionTtl;
        this.recoveryMode = recoveryMode;
        this.now = now;
    }

    public String getNamespace() { return namespace; }
    public String getKey() { return key; }
    public String getRequestHash() { return requestHash; }
    public String getRouteKey() { return routeKey; }
    public String getOwnerToken() { return ownerToken; }
    public IdempotencyMode getMode() { return mode; }
    public Duration getProcessingTimeout() { return processingTimeout; }
    public Duration getIdempotencyWindow() { return idempotencyWindow; }
    public IdempotencyWindowPolicy getWindowPolicy() { return windowPolicy; }
    public Duration getRecordRetentionTtl() { return recordRetentionTtl; }
    public IdempotencyRecoveryMode getRecoveryMode() { return recoveryMode; }
    public Instant getNow() { return now; }
}
