package com.xjtu.iron.idempotent.provider.redis.repository;

import com.xjtu.iron.idempotent.api.policy.IdempotencyWindowPolicy;
import com.xjtu.iron.idempotent.api.recovery.IdempotencyRecoveryMode;
import com.xjtu.iron.idempotent.api.repository.*;
import com.xjtu.iron.idempotent.api.repository.acquire.*;
import com.xjtu.iron.idempotent.api.repository.recovery.*;
import com.xjtu.iron.idempotent.api.repository.write.*;
import com.xjtu.iron.idempotent.api.state.IdempotencyStatus;
import com.xjtu.iron.idempotent.api.storage.IdempotencyStorageContext;
import com.xjtu.iron.idempotent.provider.redis.key.RedisIdempotencyKeyBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * WINDOWED Redis 幂等状态仓储。
 *
 * <p>V2 与 JDBC 共用 storeName/shardKey/scanBucket、四态状态机和 owner/version generation 语义。
 * Redis 当前仍不提供 Recovery 全量扫描能力，但 Hash 中会完整保存 Storage 元数据，保证协议一致。</p>
 */
public final class RedisIdempotencyRepository implements IdempotencyRepository {

    public static final String PROVIDER_NAME = "redis";

    private final StringRedisTemplate redis;
    private final RedisIdempotencyKeyBuilder keyBuilder;
    private final DefaultRedisScript<List> acquireScript;
    private final DefaultRedisScript<List> recoveryScript;
    private final DefaultRedisScript<List> successScript;
    private final DefaultRedisScript<List> failedScript;
    private final DefaultRedisScript<List> discardedScript;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public RedisIdempotencyRepository(StringRedisTemplate redis, String keyPrefix) {
        this.redis = Objects.requireNonNull(redis, "redis must not be null");
        this.keyBuilder = new RedisIdempotencyKeyBuilder(keyPrefix);
        this.acquireScript = script("META-INF/iron-idempotency/redis/try-acquire.lua");
        this.recoveryScript = script("META-INF/iron-idempotency/redis/try-recover.lua");
        this.successScript = script("META-INF/iron-idempotency/redis/mark-success.lua");
        this.failedScript = script("META-INF/iron-idempotency/redis/mark-failed.lua");
        this.discardedScript = script("META-INF/iron-idempotency/redis/mark-discarded.lua");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private DefaultRedisScript<List> script(String location) {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(location));
        script.setResultType((Class) List.class);
        return script;
    }

    @Override
    public String providerName() { return PROVIDER_NAME; }

    @Override
    public IdempotencyRepositoryCapabilities capabilities() {
        return IdempotencyRepositoryCapabilities.builder()
                .windowedSupported(true)
                .durableSupported(false)
                .resultPayloadSupported(true)
                .businessTransactionParticipationSupported(false)
                .recoveryQuerySupported(false)
                .build();
    }

    @Override
    public IdempotencyAcquireResult tryAcquire(IdempotencyAcquireRequest request) {
        if (!request.getMode().isWindowed()) {
            return IdempotencyAcquireResult.providerError(new IllegalArgumentException("redis repository supports WINDOWED only"));
        }
        try {
            IdempotencyStorageContext storage = requireStorage(request.getStorageContext());
            List<?> raw = redis.execute(
                    acquireScript,
                    Collections.singletonList(key(storage, request.getNamespace(), request.getKey())),
                    String.valueOf(request.getNow().toEpochMilli()),
                    request.getOwnerToken(),
                    empty(request.getRequestHash()),
                    empty(request.getRouteKey()),
                    String.valueOf(request.getProcessingTimeout().toMillis()),
                    String.valueOf(request.getIdempotencyWindow().toMillis()),
                    request.getWindowPolicy().name(),
                    String.valueOf(request.getRecordRetentionTtl().toMillis()),
                    request.getRecoveryMode().name(),
                    storage.getStoreName(),
                    String.valueOf(storage.getShardKey()),
                    String.valueOf(storage.getScanBucket()),
                    request.getNamespace(),
                    request.getKey());
            return parseAcquire(raw);
        } catch (Exception error) {
            return IdempotencyAcquireResult.providerError(error);
        }
    }

    @Override
    public IdempotencyRecoveryResult tryRecover(IdempotencyRecoveryAcquireRequest request) {
        if (!request.getMode().isWindowed()) {
            return IdempotencyRecoveryResult.providerError(new IllegalArgumentException("redis repository supports WINDOWED only"));
        }
        try {
            IdempotencyStorageContext storage = requireStorage(request.getStorageContext());
            List<?> raw = redis.execute(
                    recoveryScript,
                    Collections.singletonList(key(storage, request.getNamespace(), request.getKey())),
                    String.valueOf(request.getNow().toEpochMilli()),
                    request.getNewOwnerToken(),
                    empty(request.getRequestHash()),
                    empty(request.getRouteKey()),
                    String.valueOf(request.getProcessingTimeout().toMillis()),
                    request.isRecoverProcessingTimeout() ? "1" : "0",
                    request.isRecoverFailed() ? "1" : "0",
                    empty(request.getExpectedOwnerToken()),
                    request.getExpectedVersion() == null ? "" : String.valueOf(request.getExpectedVersion()),
                    String.valueOf(storage.getShardKey()),
                    String.valueOf(storage.getScanBucket()));
            return parseRecovery(raw);
        } catch (Exception error) {
            return IdempotencyRecoveryResult.providerError(error);
        }
    }

    @Override
    public IdempotencyWriteResult markSuccess(IdempotencySuccessRequest request) {
        try {
            IdempotencyStorageContext storage = requireStorage(request.getStorageContext());
            List<?> raw = redis.execute(
                    successScript,
                    Collections.singletonList(key(storage, request.getNamespace(), request.getKey())),
                    request.getOwnerToken(), String.valueOf(request.getVersion()), empty(request.getResultPayload()),
                    String.valueOf(request.getNow().toEpochMilli()), request.getMode().name(), millis(request.getIdempotencyWindow()),
                    request.getWindowPolicy().name(), String.valueOf(request.getRecordRetentionTtl().toMillis()));
            return parseWrite(raw);
        } catch (Exception error) {
            return IdempotencyWriteResult.providerError(error);
        }
    }

    @Override
    public IdempotencyWriteResult markFailed(IdempotencyFailureRequest request) {
        try {
            IdempotencyStorageContext storage = requireStorage(request.getStorageContext());
            List<?> raw = redis.execute(
                    failedScript,
                    Collections.singletonList(key(storage, request.getNamespace(), request.getKey())),
                    request.getOwnerToken(), String.valueOf(request.getVersion()), empty(request.getFailure().getCode()),
                    empty(request.getFailure().getMessage()), request.getFailure().isRetryable() ? "1" : "0",
                    String.valueOf(request.getNow().toEpochMilli()), request.getMode().name(), millis(request.getIdempotencyWindow()),
                    request.getWindowPolicy().name(), String.valueOf(request.getRecordRetentionTtl().toMillis()));
            return parseWrite(raw);
        } catch (Exception error) {
            return IdempotencyWriteResult.providerError(error);
        }
    }

    @Override
    public IdempotencyWriteResult markDiscarded(IdempotencyDiscardRequest request) {
        try {
            IdempotencyStorageContext storage = requireStorage(request.getStorageContext());
            List<?> raw = redis.execute(
                    discardedScript,
                    Collections.singletonList(key(storage, request.getNamespace(), request.getKey())),
                    request.getOwnerToken(), String.valueOf(request.getVersion()), empty(request.getResultPayload()),
                    String.valueOf(request.getNow().toEpochMilli()), request.getMode().name(), millis(request.getIdempotencyWindow()),
                    request.getWindowPolicy().name(), String.valueOf(request.getRecordRetentionTtl().toMillis()));
            return parseWrite(raw);
        } catch (Exception error) {
            return IdempotencyWriteResult.providerError(error);
        }
    }

    @Override
    public Optional<IdempotencyRecord> find(IdempotencyStorageContext storageContext, String namespace, String logicalKey) {
        IdempotencyStorageContext storage = requireStorage(storageContext);
        Map<Object, Object> values = redis.opsForHash().entries(key(storage, namespace, logicalKey));
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(fromMap(values));
    }

    private IdempotencyAcquireResult parseAcquire(List<?> raw) {
        if (raw == null || raw.isEmpty()) {
            return IdempotencyAcquireResult.providerError(new IllegalStateException("empty acquire script result"));
        }
        int code = Integer.parseInt(text(raw.get(0)));
        boolean rollover = raw.size() > 1 && "1".equals(text(raw.get(1)));
        IdempotencyRecord record = snapshot(raw, 2);
        return switch (code) {
            case 1 -> IdempotencyAcquireResult.acquired(record, rollover);
            case 2 -> IdempotencyAcquireResult.of(IdempotencyAcquireStatus.SUCCESS, record);
            case 3 -> IdempotencyAcquireResult.of(IdempotencyAcquireStatus.PROCESSING_ACTIVE, record);
            case 4 -> IdempotencyAcquireResult.of(IdempotencyAcquireStatus.PROCESSING_EXPIRED, record);
            case 5 -> IdempotencyAcquireResult.of(IdempotencyAcquireStatus.FAILED_RETRYABLE, record);
            case 6 -> IdempotencyAcquireResult.of(IdempotencyAcquireStatus.FAILED_FINAL, record);
            case 7 -> IdempotencyAcquireResult.of(IdempotencyAcquireStatus.KEY_CONFLICT, record);
            case 8 -> IdempotencyAcquireResult.of(IdempotencyAcquireStatus.DISCARDED, record);
            default -> IdempotencyAcquireResult.providerError(new IllegalStateException("unknown acquire code: " + code));
        };
    }

    private IdempotencyRecoveryResult parseRecovery(List<?> raw) {
        if (raw == null || raw.isEmpty()) {
            return IdempotencyRecoveryResult.providerError(new IllegalStateException("empty recovery script result"));
        }
        int code = Integer.parseInt(text(raw.get(0)));
        String reason = raw.size() > 1 ? nullable(text(raw.get(1))) : null;
        IdempotencyRecord record = raw.size() > 2 ? snapshot(raw, 2) : null;
        return switch (code) {
            case 1 -> IdempotencyRecoveryResult.acquired(record, reason);
            case 2 -> IdempotencyRecoveryResult.of(IdempotencyRecoveryStatus.SUCCESS, record);
            case 3 -> IdempotencyRecoveryResult.of(IdempotencyRecoveryStatus.PROCESSING_ACTIVE, record);
            case 4 -> IdempotencyRecoveryResult.of(IdempotencyRecoveryStatus.NOT_RECOVERABLE, record);
            case 5 -> IdempotencyRecoveryResult.of(IdempotencyRecoveryStatus.FAILED_FINAL, record);
            case 6 -> IdempotencyRecoveryResult.of(IdempotencyRecoveryStatus.NOT_FOUND, null);
            case 7 -> IdempotencyRecoveryResult.of(IdempotencyRecoveryStatus.KEY_CONFLICT, record);
            case 8 -> IdempotencyRecoveryResult.of(IdempotencyRecoveryStatus.STALE_CANDIDATE, record);
            case 9 -> IdempotencyRecoveryResult.of(IdempotencyRecoveryStatus.DISCARDED, record);
            default -> IdempotencyRecoveryResult.providerError(new IllegalStateException("unknown recovery code: " + code));
        };
    }

    private IdempotencyWriteResult parseWrite(List<?> raw) {
        if (raw == null || raw.isEmpty()) {
            return IdempotencyWriteResult.providerError(new IllegalStateException("empty write script result"));
        }
        int code = Integer.parseInt(text(raw.get(0)));
        IdempotencyRecord record = raw.size() > 1 ? snapshot(raw, 1) : null;
        return switch (code) {
            case 1 -> IdempotencyWriteResult.of(IdempotencyWriteStatus.UPDATED, record);
            case 2 -> IdempotencyWriteResult.of(IdempotencyWriteStatus.NOT_FOUND, null);
            case 3 -> IdempotencyWriteResult.of(IdempotencyWriteStatus.STALE_OWNER, record);
            case 4 -> IdempotencyWriteResult.of(IdempotencyWriteStatus.ALREADY_FINAL, record);
            default -> IdempotencyWriteResult.providerError(new IllegalStateException("unknown write code: " + code));
        };
    }

    /**
     * V2 snapshot 共 22 个字段：storeName,shardKey,scanBucket,namespace,key,routeKey,requestHash,status,owner,version,result,
     * failureCode,failureMessage,retryable,recoveryMode,windowPolicy,processingExpireAt,windowExpireAt,retentionExpireAt,
     * createdAt,updatedAt,completedAt。
     */
    private IdempotencyRecord snapshot(List<?> values, int offset) {
        if (values.size() < offset + 22) {
            return null;
        }
        return IdempotencyRecord.builder()
                .storeName(nullable(text(values.get(offset))))
                .shardKey(Long.parseLong(text(values.get(offset + 1))))
                .scanBucket(Integer.parseInt(text(values.get(offset + 2))))
                .namespace(nullable(text(values.get(offset + 3))))
                .key(nullable(text(values.get(offset + 4))))
                .routeKey(nullable(text(values.get(offset + 5))))
                .requestHash(nullable(text(values.get(offset + 6))))
                .status(IdempotencyStatus.valueOf(text(values.get(offset + 7))))
                .ownerToken(nullable(text(values.get(offset + 8))))
                .version(Long.parseLong(text(values.get(offset + 9))))
                .resultPayload(nullable(text(values.get(offset + 10))))
                .failureCode(nullable(text(values.get(offset + 11))))
                .failureMessage(nullable(text(values.get(offset + 12))))
                .failureRetryable("1".equals(text(values.get(offset + 13))))
                .recoveryMode(enumValue(IdempotencyRecoveryMode.class, text(values.get(offset + 14)), IdempotencyRecoveryMode.NONE))
                .windowPolicy(enumValue(IdempotencyWindowPolicy.class, text(values.get(offset + 15)), IdempotencyWindowPolicy.FIXED_FROM_FIRST_ACQUIRE))
                .processingExpireAt(epochMillis(text(values.get(offset + 16))))
                .windowExpireAt(epochMillis(text(values.get(offset + 17))))
                .retentionExpireAt(epochMillis(text(values.get(offset + 18))))
                .createdAt(epochMillis(text(values.get(offset + 19))))
                .updatedAt(epochMillis(text(values.get(offset + 20))))
                .completedAt(epochMillis(text(values.get(offset + 21))))
                .build();
    }

    private IdempotencyRecord fromMap(Map<Object, Object> values) {
        return IdempotencyRecord.builder()
                .storeName(value(values, "store_name"))
                .shardKey(Long.parseLong(value(values, "shard_key")))
                .scanBucket(Integer.parseInt(value(values, "scan_bucket")))
                .namespace(value(values, "namespace"))
                .key(value(values, "key"))
                .routeKey(nullable(value(values, "route_key")))
                .requestHash(nullable(value(values, "request_hash")))
                .status(IdempotencyStatus.valueOf(value(values, "status")))
                .ownerToken(nullable(value(values, "owner_token")))
                .version(Long.parseLong(value(values, "version")))
                .resultPayload(nullable(value(values, "result_payload")))
                .failureCode(nullable(value(values, "failure_code")))
                .failureMessage(nullable(value(values, "failure_message")))
                .failureRetryable("1".equals(value(values, "failure_retryable")))
                .recoveryMode(enumValue(IdempotencyRecoveryMode.class, value(values, "recovery_mode"), IdempotencyRecoveryMode.NONE))
                .windowPolicy(enumValue(IdempotencyWindowPolicy.class, value(values, "window_policy"), IdempotencyWindowPolicy.FIXED_FROM_FIRST_ACQUIRE))
                .processingExpireAt(epochMillis(value(values, "processing_expire_at")))
                .windowExpireAt(epochMillis(value(values, "window_expire_at")))
                .retentionExpireAt(epochMillis(value(values, "retention_expire_at")))
                .createdAt(epochMillis(value(values, "created_at")))
                .updatedAt(epochMillis(value(values, "updated_at")))
                .completedAt(epochMillis(value(values, "completed_at")))
                .build();
    }

    private String key(IdempotencyStorageContext storage, String namespace, String logicalKey) {
        return keyBuilder.build(storage.getStoreName(), namespace, logicalKey);
    }

    private IdempotencyStorageContext requireStorage(IdempotencyStorageContext storage) {
        return Objects.requireNonNull(storage, "storageContext must not be null");
    }

    private String millis(java.time.Duration value) { return value == null ? "0" : String.valueOf(value.toMillis()); }

    private String value(Map<Object, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? "" : value.toString();
    }

    private String text(Object value) { return value == null ? "" : value.toString(); }
    private String empty(String value) { return value == null ? "" : value; }
    private String nullable(String value) { return value == null || value.isBlank() ? null : value; }

    private Instant epochMillis(String value) {
        return value == null || value.isBlank() ? null : Instant.ofEpochMilli(Long.parseLong(value));
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, E defaultValue) {
        return value == null || value.isBlank() ? defaultValue : Enum.valueOf(type, value);
    }
}
