package com.xjtu.iron.idempotent.api.repository.write;

import com.xjtu.iron.idempotent.api.policy.IdempotencyMode;
import com.xjtu.iron.idempotent.api.policy.IdempotencyWindowPolicy;
import com.xjtu.iron.idempotent.api.storage.IdempotencyStorageContext;

import java.time.Duration;
import java.time.Instant;

/** PROCESSING -> DISCARDED 条件写请求。 */
public final class IdempotencyDiscardRequest {

    private final IdempotencyStorageContext storageContext;
    private final String namespace;
    private final String key;
    private final String ownerToken;
    private final long version;
    private final String resultPayload;
    private final IdempotencyMode mode;
    private final Duration idempotencyWindow;
    private final IdempotencyWindowPolicy windowPolicy;
    private final Duration recordRetentionTtl;
    private final Instant now;

    public IdempotencyDiscardRequest(IdempotencyStorageContext storageContext, String namespace, String key, String ownerToken, long version,
                                     String resultPayload, IdempotencyMode mode, Duration idempotencyWindow,
                                     IdempotencyWindowPolicy windowPolicy, Duration recordRetentionTtl, Instant now) {
        this.storageContext = storageContext;
        this.namespace = namespace;
        this.key = key;
        this.ownerToken = ownerToken;
        this.version = version;
        this.resultPayload = resultPayload;
        this.mode = mode;
        this.idempotencyWindow = idempotencyWindow;
        this.windowPolicy = windowPolicy;
        this.recordRetentionTtl = recordRetentionTtl;
        this.now = now;
    }

    public IdempotencyStorageContext getStorageContext() { return storageContext; }
    public String getNamespace() { return namespace; }
    public String getKey() { return key; }
    public String getOwnerToken() { return ownerToken; }
    public long getVersion() { return version; }
    public String getResultPayload() { return resultPayload; }
    public IdempotencyMode getMode() { return mode; }
    public Duration getIdempotencyWindow() { return idempotencyWindow; }
    public IdempotencyWindowPolicy getWindowPolicy() { return windowPolicy; }
    public Duration getRecordRetentionTtl() { return recordRetentionTtl; }
    public Instant getNow() { return now; }
}
