package com.xjtu.iron.idempotent.provider.redis;

import com.xjtu.iron.idempotent.api.IdempotencyMode;
import com.xjtu.iron.idempotent.api.IdempotencyStatus;
import com.xjtu.iron.idempotent.api.repository.IdempotencyAcquireRequest;
import com.xjtu.iron.idempotent.api.repository.IdempotencyAcquireResult;
import com.xjtu.iron.idempotent.api.repository.IdempotencyAcquireStatus;
import com.xjtu.iron.idempotent.api.repository.IdempotencyFailureRequest;
import com.xjtu.iron.idempotent.api.repository.IdempotencyRecord;
import com.xjtu.iron.idempotent.api.repository.IdempotencyRepository;
import com.xjtu.iron.idempotent.api.repository.IdempotencySuccessRequest;
import com.xjtu.iron.idempotent.api.repository.IdempotencyWriteResult;
import com.xjtu.iron.idempotent.api.repository.IdempotencyWriteStatus;
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
 * SHORT_TERM 模式的 Redis 幂等状态仓储。
 *
 * <p>Redis Hash 保存状态；Lua 把“读取状态 + 判断 + 状态转换”合并为一个原子操作。
 * {@code recordTtl} 表示有限去重窗口：SUCCESS / FAILED 后会刷新 TTL，TTL 到期后
 * 整条幂等记录被删除，同一个 key 可以再次作为新请求执行。</p>
 *
 * <p>因此该 Provider 适合按钮连点、客户端重试、短周期 requestId 去重等场景，
 * 不建议作为支付/订单等长期业务事实的唯一存储。</p>
 */
public final class RedisIdempotencyRepository implements IdempotencyRepository {

    public static final String PROVIDER_NAME = "redis";

    private final StringRedisTemplate redis;
    private final RedisIdempotencyKeyBuilder keyBuilder;
    private final DefaultRedisScript<List> acquireScript;
    private final DefaultRedisScript<List> successScript;
    private final DefaultRedisScript<List> failedScript;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public RedisIdempotencyRepository(StringRedisTemplate redis, String keyPrefix) {
        this.redis = Objects.requireNonNull(redis, "redis must not be null");
        this.keyBuilder = new RedisIdempotencyKeyBuilder(keyPrefix);

        this.acquireScript = new DefaultRedisScript<>();
        this.acquireScript.setLocation(new ClassPathResource(
                "META-INF/iron-idempotency/redis/try-acquire.lua"));
        this.acquireScript.setResultType((Class) List.class);

        this.successScript = new DefaultRedisScript<>();
        this.successScript.setLocation(new ClassPathResource(
                "META-INF/iron-idempotency/redis/mark-success.lua"));
        this.successScript.setResultType((Class) List.class);

        this.failedScript = new DefaultRedisScript<>();
        this.failedScript.setLocation(new ClassPathResource(
                "META-INF/iron-idempotency/redis/mark-failed.lua"));
        this.failedScript.setResultType((Class) List.class);
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean supports(IdempotencyMode mode) {
        return mode == IdempotencyMode.SHORT_TERM;
    }

    @Override
    public IdempotencyAcquireResult tryAcquire(IdempotencyAcquireRequest request) {
        if (request.getMode() != IdempotencyMode.SHORT_TERM) {
            return IdempotencyAcquireResult.providerError(
                    new IllegalArgumentException("redis repository supports SHORT_TERM only in V1"));
        }

        try {
            List<?> raw = redis.execute(
                    acquireScript,
                    Collections.singletonList(keyBuilder.build(request.getNamespace(), request.getKey())),
                    String.valueOf(request.getNow().toEpochMilli()),
                    request.getOwnerToken(),
                    nullToEmpty(request.getRequestHash()),
                    String.valueOf(request.getProcessingTimeout().toMillis()),
                    String.valueOf(request.getRecordTtl().toMillis()),
                    request.isRetryFailed() ? "1" : "0",
                    request.isRetryOnProcessingTimeout() ? "1" : "0",
                    request.getNamespace(),
                    request.getKey());

            return parseAcquire(raw);
        } catch (Exception error) {
            return IdempotencyAcquireResult.providerError(error);
        }
    }

    @Override
    public IdempotencyWriteResult markSuccess(IdempotencySuccessRequest request) {
        try {
            List<?> raw = redis.execute(
                    successScript,
                    Collections.singletonList(keyBuilder.build(request.getNamespace(), request.getKey())),
                    request.getOwnerToken(),
                    String.valueOf(request.getVersion()),
                    nullToEmpty(request.getResultPayload()),
                    String.valueOf(request.getNow().toEpochMilli()),
                    String.valueOf(request.getRecordTtl().toMillis()));

            return parseWrite(raw);
        } catch (Exception error) {
            return IdempotencyWriteResult.providerError(error);
        }
    }

    @Override
    public IdempotencyWriteResult markFailed(IdempotencyFailureRequest request) {
        try {
            List<?> raw = redis.execute(
                    failedScript,
                    Collections.singletonList(keyBuilder.build(request.getNamespace(), request.getKey())),
                    request.getOwnerToken(),
                    String.valueOf(request.getVersion()),
                    nullToEmpty(request.getFailure().getCode()),
                    nullToEmpty(request.getFailure().getMessage()),
                    request.getFailure().isRetryable() ? "1" : "0",
                    String.valueOf(request.getNow().toEpochMilli()),
                    String.valueOf(request.getRecordTtl().toMillis()));

            return parseWrite(raw);
        } catch (Exception error) {
            return IdempotencyWriteResult.providerError(error);
        }
    }

    @Override
    public Optional<IdempotencyRecord> find(String namespace, String key) {
        Map<Object, Object> values = redis.opsForHash().entries(keyBuilder.build(namespace, key));
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(fromMap(values));
    }

    /**
     * Lua acquire 返回码：
     * 1=ACQUIRED, 2=SUCCESS, 3=PROCESSING, 4=FAILED, 5=KEY_CONFLICT。
     */
    private IdempotencyAcquireResult parseAcquire(List<?> raw) {
        if (raw == null || raw.isEmpty()) {
            return IdempotencyAcquireResult.providerError(
                    new IllegalStateException("empty acquire script result"));
        }

        int code = Integer.parseInt(text(raw.get(0)));
        boolean takeover = raw.size() > 1 && "1".equals(text(raw.get(1)));
        IdempotencyRecord record = snapshot(raw, 2);

        return switch (code) {
            case 1 -> IdempotencyAcquireResult.acquired(record, takeover);
            case 2 -> IdempotencyAcquireResult.of(IdempotencyAcquireStatus.SUCCESS, record);
            case 3 -> IdempotencyAcquireResult.of(IdempotencyAcquireStatus.PROCESSING, record);
            case 4 -> IdempotencyAcquireResult.of(IdempotencyAcquireStatus.FAILED, record);
            case 5 -> IdempotencyAcquireResult.of(IdempotencyAcquireStatus.KEY_CONFLICT, record);
            default -> IdempotencyAcquireResult.providerError(
                    new IllegalStateException("unknown acquire code: " + code));
        };
    }

    /**
     * Lua final-write 返回码：
     * 1=UPDATED, 2=NOT_FOUND, 3=STALE_OWNER, 4=ALREADY_FINAL。
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
     * Script snapshot 字段顺序：
     * namespace,key,requestHash,status,owner,version,result,failureCode,
     * failureMessage,retryable,expire,created,updated,completed。
     */
    private IdempotencyRecord snapshot(List<?> values, int offset) {
        if (values.size() < offset + 14) {
            return null;
        }
        return IdempotencyRecord.builder()
                .namespace(nullable(text(values.get(offset))))
                .key(nullable(text(values.get(offset + 1))))
                .requestHash(nullable(text(values.get(offset + 2))))
                .status(IdempotencyStatus.valueOf(text(values.get(offset + 3))))
                .ownerToken(nullable(text(values.get(offset + 4))))
                .version(Long.parseLong(text(values.get(offset + 5))))
                .resultPayload(nullable(text(values.get(offset + 6))))
                .failureCode(nullable(text(values.get(offset + 7))))
                .failureMessage(nullable(text(values.get(offset + 8))))
                .failureRetryable("1".equals(text(values.get(offset + 9))))
                .processingExpireAt(epochMillis(text(values.get(offset + 10))))
                .createdAt(epochMillis(text(values.get(offset + 11))))
                .updatedAt(epochMillis(text(values.get(offset + 12))))
                .completedAt(epochMillis(text(values.get(offset + 13))))
                .build();
    }

    private IdempotencyRecord fromMap(Map<Object, Object> values) {
        return IdempotencyRecord.builder()
                .namespace(value(values, "namespace"))
                .key(value(values, "key"))
                .requestHash(nullable(value(values, "request_hash")))
                .status(IdempotencyStatus.valueOf(value(values, "status")))
                .ownerToken(nullable(value(values, "owner_token")))
                .version(Long.parseLong(value(values, "version")))
                .resultPayload(nullable(value(values, "result_payload")))
                .failureCode(nullable(value(values, "failure_code")))
                .failureMessage(nullable(value(values, "failure_message")))
                .failureRetryable("1".equals(value(values, "failure_retryable")))
                .processingExpireAt(epochMillis(value(values, "processing_expire_at")))
                .createdAt(epochMillis(value(values, "created_at")))
                .updatedAt(epochMillis(value(values, "updated_at")))
                .completedAt(epochMillis(value(values, "completed_at")))
                .build();
    }

    private String value(Map<Object, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? "" : value.toString();
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String nullable(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private Instant epochMillis(String value) {
        return value == null || value.isBlank()
                ? null
                : Instant.ofEpochMilli(Long.parseLong(value));
    }
}
