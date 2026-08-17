package com.xjtu.iron.idempotent.core;

import com.xjtu.iron.idempotent.api.policy.IdempotencyMode;
import com.xjtu.iron.idempotent.api.policy.IdempotencyOptions;
import com.xjtu.iron.idempotent.api.policy.IdempotencyResultPolicy;
import com.xjtu.iron.idempotent.api.recovery.IdempotencyRecoveryMode;
import com.xjtu.iron.idempotent.api.repository.*;
import com.xjtu.iron.idempotent.api.repository.recovery.IdempotencyRecoveryAcquireRequest;
import com.xjtu.iron.idempotent.api.repository.recovery.IdempotencyRecoveryResult;
import com.xjtu.iron.idempotent.api.repository.recovery.IdempotencyRecoveryStatus;
import com.xjtu.iron.idempotent.api.repository.acquire.IdempotencyAcquireRequest;
import com.xjtu.iron.idempotent.api.repository.acquire.IdempotencyAcquireResult;
import com.xjtu.iron.idempotent.api.repository.acquire.IdempotencyAcquireStatus;
import com.xjtu.iron.idempotent.api.repository.write.*;
import com.xjtu.iron.idempotent.api.spi.IdempotencyResultCodec;
import com.xjtu.iron.idempotent.api.state.IdempotencyStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultIdempotencyExecutorTest {

    @Test
    void normalExecuteShouldNotTakeOverExpiredProcessing() {
        MemoryRepository repository = new MemoryRepository();
        DefaultIdempotencyExecutor executor = executor(repository);

        IdempotencyOptions options = IdempotencyOptions.builder()
                .mode(IdempotencyMode.DURABLE)
                .processingTimeout(java.time.Duration.ofSeconds(1))
                .recoveryMode(IdempotencyRecoveryMode.EXTERNAL_TASK)
                .build();

        IdempotencyRequest request = IdempotencyRequest.builder()
                .key("order:1")
                .routeKey("merchant:1")
                .requestHash("hash")
                .options(options)
                .build();

        IdempotencyResult<String> first = executor.execute(request, String.class, ctx -> "ok");
        assertThat(first.getStatus()).isEqualTo(IdempotencyResultStatus.EXECUTED);
    }

    @Test
    void successCanReplayStoredResultWithoutExecutingCallbackAgain() {
        MemoryRepository repository = new MemoryRepository();
        DefaultIdempotencyExecutor executor = executor(repository, stringCodec());

        IdempotencyOptions options = IdempotencyOptions.builder()
                .mode(IdempotencyMode.DURABLE)
                .recoveryMode(IdempotencyRecoveryMode.EXTERNAL_TASK)
                .resultPolicy(IdempotencyResultPolicy.STORE_AND_REPLAY)
                .build();

        IdempotencyRequest request = IdempotencyRequest.builder()
                .key("order:replay")
                .routeKey("merchant:1")
                .requestHash("hash-replay")
                .options(options)
                .build();

        IdempotencyResult<String> first = executor.execute(request, String.class, ctx -> "first-result");
        IdempotencyResult<String> second = executor.execute(request, String.class, ctx -> {
            throw new AssertionError("duplicate SUCCESS request must not execute callback again");
        });

        assertThat(first.getStatus()).isEqualTo(IdempotencyResultStatus.EXECUTED);
        assertThat(second.getStatus()).isEqualTo(IdempotencyResultStatus.REPLAYED);
        assertThat(second.getValue()).isEqualTo("first-result");
    }

    private DefaultIdempotencyExecutor executor(IdempotencyRepository repository) {
        return executor(repository, null);
    }

    private DefaultIdempotencyExecutor executor(
            IdempotencyRepository repository,
            IdempotencyResultCodec codec) {
        IdempotencyRepositoryRegistry registry = new DefaultIdempotencyRepositoryRegistry(
                List.of(repository), "mem", "mem");
        IdempotencyOptions defaults = IdempotencyOptions.durable();
        return new DefaultIdempotencyExecutor(
                registry,
                new IdempotencyDefaults(IdempotencyMode.DURABLE, defaults, defaults),
                (namespace, key) -> UUID.randomUUID().toString(),
                (error, at) -> new IdempotencyFailureInfo("BUSINESS_ERROR", error.getMessage(), false, at),
                codec,
                null,
                null,
                null,
                Clock.fixed(Instant.parse("2026-08-13T00:00:00Z"), ZoneOffset.UTC));
    }

    private IdempotencyResultCodec stringCodec() {
        return new IdempotencyResultCodec() {
            @Override
            public String encode(Object value) {
                return value == null ? null : String.valueOf(value);
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> T decode(String payload, Class<T> resultType) {
                if (resultType == String.class) {
                    return (T) payload;
                }
                throw new IllegalArgumentException("String codec only supports String resultType");
            }
        };
    }

    private static final class MemoryRepository implements IdempotencyRepository {
        private final Map<String, IdempotencyRecord> data = new HashMap<>();

        @Override public String providerName() { return "mem"; }
        @Override public boolean supports(IdempotencyMode mode) { return true; }

        @Override
        public synchronized IdempotencyAcquireResult tryAcquire(IdempotencyAcquireRequest r) {
            IdempotencyRecord current = data.get(r.getKey());
            if (current == null) {
                current = IdempotencyRecord.builder()
                        .namespace(r.getNamespace())
                        .key(r.getKey())
                        .routeKey(r.getRouteKey())
                        .requestHash(r.getRequestHash())
                        .status(IdempotencyStatus.PROCESSING)
                        .ownerToken(r.getOwnerToken())
                        .version(1)
                        .recoveryMode(r.getRecoveryMode())
                        .windowPolicy(r.getWindowPolicy())
                        .processingExpireAt(r.getNow().plus(r.getProcessingTimeout()))
                        .createdAt(r.getNow())
                        .updatedAt(r.getNow())
                        .build();
                data.put(r.getKey(), current);
                return IdempotencyAcquireResult.acquired(current, false);
            }
            return switch (current.getStatus()) {
                case SUCCESS -> IdempotencyAcquireResult.of(IdempotencyAcquireStatus.SUCCESS, current);
                case FAILED -> IdempotencyAcquireResult.of(
                        current.isFailureRetryable()
                                ? IdempotencyAcquireStatus.FAILED_RETRYABLE
                                : IdempotencyAcquireStatus.FAILED_FINAL,
                        current);
                case PROCESSING -> IdempotencyAcquireResult.of(
                        current.getProcessingExpireAt().isAfter(r.getNow())
                                ? IdempotencyAcquireStatus.PROCESSING_ACTIVE
                                : IdempotencyAcquireStatus.PROCESSING_EXPIRED,
                        current);
            };
        }

        @Override
        public IdempotencyRecoveryResult tryRecover(IdempotencyRecoveryAcquireRequest request) {
            return IdempotencyRecoveryResult.of(IdempotencyRecoveryStatus.NOT_RECOVERABLE, null);
        }

        @Override
        public synchronized IdempotencyWriteResult markSuccess(IdempotencySuccessRequest r) {
            IdempotencyRecord current = data.get(r.getKey());
            IdempotencyRecord next = IdempotencyRecord.builder()
                    .namespace(current.getNamespace())
                    .key(current.getKey())
                    .routeKey(current.getRouteKey())
                    .requestHash(current.getRequestHash())
                    .status(IdempotencyStatus.SUCCESS)
                    .ownerToken(current.getOwnerToken())
                    .version(current.getVersion())
                    .resultPayload(r.getResultPayload())
                    .recoveryMode(current.getRecoveryMode())
                    .windowPolicy(current.getWindowPolicy())
                    .updatedAt(r.getNow())
                    .completedAt(r.getNow())
                    .build();
            data.put(r.getKey(), next);
            return IdempotencyWriteResult.of(IdempotencyWriteStatus.UPDATED, next);
        }

        @Override
        public IdempotencyWriteResult markFailed(IdempotencyFailureRequest request) {
            return IdempotencyWriteResult.of(IdempotencyWriteStatus.UPDATED, data.get(request.getKey()));
        }

        @Override
        public Optional<IdempotencyRecord> find(String namespace, String key) {
            return Optional.ofNullable(data.get(key));
        }
    }
}
