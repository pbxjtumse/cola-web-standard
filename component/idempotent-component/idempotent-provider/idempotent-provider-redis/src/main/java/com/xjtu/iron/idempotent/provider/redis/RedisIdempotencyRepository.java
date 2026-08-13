package com.xjtu.iron.idempotent.provider.redis;

import com.xjtu.iron.idempotent.api.IdempotencyMode;
import com.xjtu.iron.idempotent.api.IdempotencyRecoveryMode;
import com.xjtu.iron.idempotent.api.IdempotencyStatus;
import com.xjtu.iron.idempotent.api.IdempotencyWindowPolicy;
import com.xjtu.iron.idempotent.api.repository.*;
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
 * SHORT_TERM Redis 幂等状态仓储。
 *
 * <p>V1.1 不再把 Redis key TTL 直接等同于“幂等窗口”：</p>
 * <ul>
 *     <li>window_expire_at：语义窗口；</li>
 *     <li>retention_expire_at：物理记录保留截止时间；</li>
 *     <li>Redis PEXPIREAT 使用 retention_expire_at；</li>
 *     <li>窗口已结束但记录还因 retention 存在时，下一请求开启新的 generation。</li>
 * </ul>
 */
public final class RedisIdempotencyRepository implements IdempotencyRepository {

    public static final String PROVIDER_NAME = "redis";

    private final StringRedisTemplate redis;
    private final RedisIdempotencyKeyBuilder keyBuilder;
    private final DefaultRedisScript<List> acquireScript;
    private final DefaultRedisScript<List> recoveryScript;
    private final DefaultRedisScript<List> successScript;
    private final DefaultRedisScript<List> failedScript;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public RedisIdempotencyRepository(StringRedisTemplate redis, String keyPrefix) {
        this.redis = Objects.requireNonNull(redis, "redis must not be null");
        this.keyBuilder = new RedisIdempotencyKeyBuilder(keyPrefix);
        this.acquireScript = script("META-INF/iron-idempotency/redis/try-acquire.lua");
        this.recoveryScript = script("META-INF/iron-idempotency/redis/try-recover.lua");
        this.successScript = script("META-INF/iron-idempotency/redis/mark-success.lua");
        this.failedScript = script("META-INF/iron-idempotency/redis/mark-failed.lua");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private DefaultRedisScript<List> script(String location) {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(location));
        script.setResultType((Class) List.class);
        return script;
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean supports(IdempotencyMode mode) {
        return mode == IdempotencyMode.SHORT_TERM;
    }

    /**
     * SHORT_TERM 普通状态抢占。
     *
     * <p>读取、窗口判断、requestHash/routeKey 冲突判断、PROCESSING 判定、新 generation 创建
     * 全部放在一个 Lua 中执行，避免 GET -> Java 判断 -> SET 之间插入其他并发请求。</p>
     */
    @Override
    public IdempotencyAcquireResult tryAcquire(IdempotencyAcquireRequest request) {
        if (request.getMode() != IdempotencyMode.SHORT_TERM) {
            return IdempotencyAcquireResult.providerError(
                    new IllegalArgumentException("redis repository supports SHORT_TERM only"));
        }
        try {
            List<?> raw = redis.execute(
                    acquireScript,
                    Collections.singletonList(key(request.getNamespace(), request.getKey())),
                    String.valueOf(request.getNow().toEpochMilli()),
                    request.getOwnerToken(),
                    empty(request.getRequestHash()),
                    empty(request.getRouteKey()),
                    String.valueOf(request.getProcessingTimeout().toMillis()),
                    String.valueOf(request.getIdempotencyWindow().toMillis()),
                    request.getWindowPolicy().name(),
                    String.valueOf(request.getRecordRetentionTtl().toMillis()),
                    request.getRecoveryMode().name(),
                    request.getNamespace(),
                    request.getKey());
            return parseAcquire(raw);
        } catch (Exception error) {
            return IdempotencyAcquireResult.providerError(error);
        }
    }

    /**
     * SHORT_TERM 显式恢复抢占。
     *
     * <p>默认 SHORT_TERM recoveryMode=NONE，因此通常不会调用；只有业务明确启用 EXTERNAL_TASK 时，
     * Reliable Task 才会使用该原子脚本接管超时 PROCESSING / 可恢复 FAILED。</p>
     */
    @Override
    public IdempotencyRecoveryResult tryRecover(IdempotencyRecoveryAcquireRequest request) {
        if (request.getMode() != IdempotencyMode.SHORT_TERM) {
            return IdempotencyRecoveryResult.providerError(
                    new IllegalArgumentException("redis repository supports SHORT_TERM only"));
        }
        try {
            List<?> raw = redis.execute(
                    recoveryScript,
                    Collections.singletonList(key(request.getNamespace(), request.getKey())),
                    String.valueOf(request.getNow().toEpochMilli()),
                    request.getNewOwnerToken(),
                    empty(request.getRequestHash()),
                    empty(request.getRouteKey()),
                    String.valueOf(request.getProcessingTimeout().toMillis()),
                    request.isRecoverFailed() ? "1" : "0",
                    empty(request.getExpectedOwnerToken()),
                    request.getExpectedVersion() == null ? "" : String.valueOf(request.getExpectedVersion()));
            return parseRecovery(raw);
        } catch (Exception error) {
            return IdempotencyRecoveryResult.providerError(error);
        }
    }

    /**
     * Lua 原子完成 PROCESSING -> SUCCESS。
     *
     * <p>脚本必须同时校验 ownerToken + version，防止已经失效的旧 generation 写入成功结果。</p>
     */
    @Override
    public IdempotencyWriteResult markSuccess(IdempotencySuccessRequest request) {
        try {
            List<?> raw = redis.execute(
                    successScript,
                    Collections.singletonList(key(request.getNamespace(), request.getKey())),
                    request.getOwnerToken(),
                    String.valueOf(request.getVersion()),
                    empty(request.getResultPayload()),
                    String.valueOf(request.getNow().toEpochMilli()),
                    request.getMode().name(),
                    millis(request.getIdempotencyWindow()),
                    request.getWindowPolicy().name(),
                    String.valueOf(request.getRecordRetentionTtl().toMillis()));
            return parseWrite(raw);
        } catch (Exception error) {
            return IdempotencyWriteResult.providerError(error);
        }
    }

    /**
     * Lua 原子完成 PROCESSING -> FAILED，并保存 failureCode / retryable。
     * 普通 execute() 不会因为 retryable=true 自动重试。</p>
     */
    @Override
    public IdempotencyWriteResult markFailed(IdempotencyFailureRequest request) {
        try {
            List<?> raw = redis.execute(
                    failedScript,
                    Collections.singletonList(key(request.getNamespace(), request.getKey())),
                    request.getOwnerToken(),
                    String.valueOf(request.getVersion()),
                    empty(request.getFailure().getCode()),
                    empty(request.getFailure().getMessage()),
                    request.getFailure().isRetryable() ? "1" : "0",
                    String.valueOf(request.getNow().toEpochMilli()),
                    request.getMode().name(),
                    millis(request.getIdempotencyWindow()),
                    request.getWindowPolicy().name(),
                    String.valueOf(request.getRecordRetentionTtl().toMillis()));
            return parseWrite(raw);
        } catch (Exception error) {
            return IdempotencyWriteResult.providerError(error);
        }
    }

    /**
     * 查询当前 Redis Hash 快照。
     *
     * <p>该方法只用于查询/诊断，不应拿它实现“先 find 再 update”的并发状态机；
     * 所有状态转换必须继续走 Lua。</p>
     */
    @Override
    public Optional<IdempotencyRecord> find(String namespace, String key) {
        Map<Object, Object> values = redis.opsForHash().entries(key(namespace, key));
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(fromMap(values));
    }

    /**
     * 把 Lua 数字协议翻译成 Java 决策对象。
     *
     * <p>脚本返回数字而不是字符串枚举，是为了让 Lua 返回协议简单稳定；
     * 这里是唯一的协议映射边界。</p>
     *
     * <p>1 ACQUIRED, 2 SUCCESS, 3 ACTIVE, 4 EXPIRED,
     * 5 FAILED_RETRYABLE, 6 FAILED_FINAL, 7 CONFLICT。</p>
     */
    private IdempotencyAcquireResult parseAcquire(List<?> raw) {
        if (raw == null || raw.isEmpty()) {
            return IdempotencyAcquireResult.providerError(
                    new IllegalStateException("empty acquire script result"));
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
            default -> IdempotencyAcquireResult.providerError(
                    new IllegalStateException("unknown acquire code: " + code));
        };
    }

    /**
     * 恢复 Lua 返回码协议。
     *
     * <p>1 ACQUIRED, 2 SUCCESS, 3 ACTIVE, 4 NOT_RECOVERABLE, 5 FAILED_FINAL,
     * 6 NOT_FOUND, 7 CONFLICT, 8 STALE。</p>
     */
    private IdempotencyRecoveryResult parseRecovery(List<?> raw) {
        if (raw == null || raw.isEmpty()) {
            return IdempotencyRecoveryResult.providerError(
                    new IllegalStateException("empty recovery script result"));
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
            default -> IdempotencyRecoveryResult.providerError(
                    new IllegalStateException("unknown recovery code: " + code));
        };
    }

    /**
     * markSuccess/markFailed Lua 返回码协议。
     *
     * <p>1 UPDATED, 2 NOT_FOUND, 3 STALE_OWNER, 4 ALREADY_FINAL。</p>
     */
    private IdempotencyWriteResult parseWrite(List<?> raw) {
        if (raw == null || raw.isEmpty()) {
            return IdempotencyWriteResult.providerError(
                    new IllegalStateException("empty write script result"));
        }
        int code = Integer.parseInt(text(raw.get(0)));
        IdempotencyRecord record = raw.size() > 1 ? snapshot(raw, 1) : null;
        return switch (code) {
            case 1 -> IdempotencyWriteResult.of(IdempotencyWriteStatus.UPDATED, record);
            case 2 -> IdempotencyWriteResult.of(IdempotencyWriteStatus.NOT_FOUND, null);
            case 3 -> IdempotencyWriteResult.of(IdempotencyWriteStatus.STALE_OWNER, record);
            case 4 -> IdempotencyWriteResult.of(IdempotencyWriteStatus.ALREADY_FINAL, record);
            default -> IdempotencyWriteResult.providerError(
                    new IllegalStateException("unknown write code: " + code));
        };
    }

    /**
     * snapshot 字段顺序共 19 个：namespace,key,routeKey,requestHash,status,owner,version,result,
     * failureCode,failureMessage,retryable,recoveryMode,windowPolicy,processingExpireAt,
     * windowExpireAt,retentionExpireAt,createdAt,updatedAt,completedAt。
     */
    private IdempotencyRecord snapshot(List<?> values, int offset) {
        if (values.size() < offset + 19) {
            return null;
        }
        return IdempotencyRecord.builder()
                .namespace(nullable(text(values.get(offset))))
                .key(nullable(text(values.get(offset + 1))))
                .routeKey(nullable(text(values.get(offset + 2))))
                .requestHash(nullable(text(values.get(offset + 3))))
                .status(IdempotencyStatus.valueOf(text(values.get(offset + 4))))
                .ownerToken(nullable(text(values.get(offset + 5))))
                .version(Long.parseLong(text(values.get(offset + 6))))
                .resultPayload(nullable(text(values.get(offset + 7))))
                .failureCode(nullable(text(values.get(offset + 8))))
                .failureMessage(nullable(text(values.get(offset + 9))))
                .failureRetryable("1".equals(text(values.get(offset + 10))))
                .recoveryMode(enumValue(IdempotencyRecoveryMode.class,
                        text(values.get(offset + 11)), IdempotencyRecoveryMode.NONE))
                .windowPolicy(enumValue(IdempotencyWindowPolicy.class,
                        text(values.get(offset + 12)), IdempotencyWindowPolicy.FIXED_FROM_FIRST_ACQUIRE))
                .processingExpireAt(epochMillis(text(values.get(offset + 13))))
                .windowExpireAt(epochMillis(text(values.get(offset + 14))))
                .retentionExpireAt(epochMillis(text(values.get(offset + 15))))
                .createdAt(epochMillis(text(values.get(offset + 16))))
                .updatedAt(epochMillis(text(values.get(offset + 17))))
                .completedAt(epochMillis(text(values.get(offset + 18))))
                .build();
    }

    private IdempotencyRecord fromMap(Map<Object, Object> values) {
        return IdempotencyRecord.builder()
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
                .recoveryMode(enumValue(IdempotencyRecoveryMode.class,
                        value(values, "recovery_mode"), IdempotencyRecoveryMode.NONE))
                .windowPolicy(enumValue(IdempotencyWindowPolicy.class,
                        value(values, "window_policy"), IdempotencyWindowPolicy.FIXED_FROM_FIRST_ACQUIRE))
                .processingExpireAt(epochMillis(value(values, "processing_expire_at")))
                .windowExpireAt(epochMillis(value(values, "window_expire_at")))
                .retentionExpireAt(epochMillis(value(values, "retention_expire_at")))
                .createdAt(epochMillis(value(values, "created_at")))
                .updatedAt(epochMillis(value(values, "updated_at")))
                .completedAt(epochMillis(value(values, "completed_at")))
                .build();
    }

    private String key(String namespace, String logicalKey) {
        return keyBuilder.build(namespace, logicalKey);
    }

    private String millis(java.time.Duration value) {
        return value == null ? "0" : String.valueOf(value.toMillis());
    }

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
