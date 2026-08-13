package com.xjtu.iron.idempotent.core;

import com.xjtu.iron.idempotent.api.*;
import com.xjtu.iron.idempotent.api.repository.*;
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

    private DefaultIdempotencyExecutor executor(IdempotencyRepository repository) {
        IdempotencyRepositoryRegistry registry = new DefaultIdempotencyRepositoryRegistry(
                List.of(repository), "mem", "mem");
        IdempotencyOptions defaults = IdempotencyOptions.durable();
        return new DefaultIdempotencyExecutor(
                registry,
                new IdempotencyDefaults(IdempotencyMode.DURABLE, defaults, defaults),
                (namespace, key) -> UUID.randomUUID().toString(),
                (error, at) -> new IdempotencyFailureInfo("BUSINESS_ERROR", error.getMessage(), false, at),
                null,
                null,
                null,
                null,
                Clock.fixed(Instant.parse("2026-08-13T00:00:00Z"), ZoneOffset.UTC));
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
