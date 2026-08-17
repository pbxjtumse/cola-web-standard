package com.xjtu.iron.idempotent.provider.jdbc;

import com.xjtu.iron.idempotent.api.policy.IdempotencyMode;
import com.xjtu.iron.idempotent.api.recovery.IdempotencyRecoveryMode;
import com.xjtu.iron.idempotent.api.policy.IdempotencyWindowPolicy;
import com.xjtu.iron.idempotent.api.repository.*;
import com.xjtu.iron.idempotent.api.repository.recovery.IdempotencyRecoveryAcquireRequest;
import com.xjtu.iron.idempotent.api.repository.recovery.IdempotencyRecoveryResult;
import com.xjtu.iron.idempotent.api.repository.recovery.IdempotencyRecoveryStatus;
import com.xjtu.iron.idempotent.api.repository.acquire.IdempotencyAcquireRequest;
import com.xjtu.iron.idempotent.api.repository.acquire.IdempotencyAcquireResult;
import com.xjtu.iron.idempotent.api.repository.acquire.IdempotencyAcquireStatus;
import com.xjtu.iron.idempotent.api.repository.write.IdempotencyFailureInfo;
import com.xjtu.iron.idempotent.api.repository.write.IdempotencyFailureRequest;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcIdempotencyRepositoryTest {

    private JdbcIdempotencyRepository repository;

    @BeforeEach
    void setup() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:idem" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");

        try (Connection connection = dataSource.getConnection()) {
            String sql = new String(
                    getClass().getResourceAsStream(
                                    "/META-INF/iron-idempotency/jdbc/schema-h2.sql")
                            .readAllBytes(),
                    StandardCharsets.UTF_8);
            for (String statement : sql.split(";")) {
                if (!statement.isBlank()) {
                    connection.createStatement().execute(statement);
                }
            }
        }
        repository = new JdbcIdempotencyRepository(dataSource, "iron_idempotency_record");
    }

    @Test
    void normalRequestSeesActiveProcessing() {
        Instant now = Instant.now();
        assertThat(repository.tryAcquire(acquire("A", now, Duration.ofSeconds(30))).getStatus())
                .isEqualTo(IdempotencyAcquireStatus.ACQUIRED);
        assertThat(repository.tryAcquire(acquire("B", now.plusMillis(1), Duration.ofSeconds(30))).getStatus())
                .isEqualTo(IdempotencyAcquireStatus.PROCESSING_ACTIVE);
    }

    @Test
    void normalRequestDoesNotTakeOverExpiredProcessing() {
        Instant now = Instant.now();
        IdempotencyAcquireResult first = repository.tryAcquire(
                acquire("A", now, Duration.ofMillis(10)));

        IdempotencyAcquireResult second = repository.tryAcquire(
                acquire("B", now.plusSeconds(1), Duration.ofSeconds(30)));

        assertThat(second.getStatus()).isEqualTo(IdempotencyAcquireStatus.PROCESSING_EXPIRED);
        assertThat(second.getRecord().getOwnerToken()).isEqualTo("A");
        assertThat(second.getRecord().getVersion()).isEqualTo(first.getRecord().getVersion());
    }

    @Test
    void recoverCanTakeOverExpiredProcessingAndRejectStaleTask() {
        Instant now = Instant.now();
        IdempotencyAcquireResult first = repository.tryAcquire(
                acquire("A", now, Duration.ofMillis(10)));

        IdempotencyRecoveryResult recovered = repository.tryRecover(
                recover("B", first.getRecord(), now.plusSeconds(1)));

        assertThat(recovered.getStatus()).isEqualTo(IdempotencyRecoveryStatus.RECOVERY_ACQUIRED);
        assertThat(recovered.getRecoveryReason()).isEqualTo("PROCESSING_TIMEOUT");
        assertThat(recovered.getRecord().getOwnerToken()).isEqualTo("B");
        assertThat(recovered.getRecord().getVersion()).isEqualTo(2);

        IdempotencyRecoveryResult stale = repository.tryRecover(
                recover("C", first.getRecord(), now.plusSeconds(2)));
        assertThat(stale.getStatus()).isEqualTo(IdempotencyRecoveryStatus.STALE_CANDIDATE);
    }

    @Test
    void retryableFailedIsDecisionOnlyUntilRecoverIsCalled() {
        Instant now = Instant.now();
        IdempotencyAcquireResult first = repository.tryAcquire(
                acquire("A", now, Duration.ofSeconds(30)));

        repository.markFailed(new IdempotencyFailureRequest(
                "n", "k", "A", first.getRecord().getVersion(),
                new IdempotencyFailureInfo("TEMP", "temporary", true, now.plusSeconds(1)),
                IdempotencyMode.DURABLE, null,
                IdempotencyWindowPolicy.FIXED_FROM_FIRST_ACQUIRE,
                Duration.ZERO, now.plusSeconds(1)));

        IdempotencyAcquireResult normal = repository.tryAcquire(
                acquire("B", now.plusSeconds(2), Duration.ofSeconds(30)));
        assertThat(normal.getStatus()).isEqualTo(IdempotencyAcquireStatus.FAILED_RETRYABLE);

        IdempotencyRecord candidate = normal.getRecord();
        IdempotencyRecoveryResult recovered = repository.tryRecover(
                recover("B", candidate, now.plusSeconds(3)));
        assertThat(recovered.getStatus()).isEqualTo(IdempotencyRecoveryStatus.RECOVERY_ACQUIRED);
        assertThat(recovered.getRecord().getVersion()).isEqualTo(2);
    }

    private IdempotencyAcquireRequest acquire(String owner, Instant now, Duration timeout) {
        return new IdempotencyAcquireRequest(
                "n", "k", "hash", "merchant:1", owner,
                IdempotencyMode.DURABLE,
                timeout,
                null,
                IdempotencyWindowPolicy.FIXED_FROM_FIRST_ACQUIRE,
                Duration.ZERO,
                IdempotencyRecoveryMode.EXTERNAL_TASK,
                now);
    }

    private IdempotencyRecoveryAcquireRequest recover(
            String newOwner,
            IdempotencyRecord candidate,
            Instant now) {
        return new IdempotencyRecoveryAcquireRequest(
                "n", "k", "hash", "merchant:1", newOwner,
                candidate.getOwnerToken(), candidate.getVersion(),
                IdempotencyMode.DURABLE,
                Duration.ofSeconds(30),
                true,
                now);
    }
}
