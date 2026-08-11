package com.xjtu.iron.idempotent.provider.jdbc;

import com.xjtu.iron.idempotent.api.IdempotencyMode;
import com.xjtu.iron.idempotent.api.IdempotencyStatus;
import com.xjtu.iron.idempotent.api.repository.IdempotencyAcquireRequest;
import com.xjtu.iron.idempotent.api.repository.IdempotencyAcquireResult;
import com.xjtu.iron.idempotent.api.repository.IdempotencyAcquireStatus;
import com.xjtu.iron.idempotent.api.repository.IdempotencyFailureInfo;
import com.xjtu.iron.idempotent.api.repository.IdempotencyFailureRequest;
import com.xjtu.iron.idempotent.api.repository.IdempotencyRecord;
import com.xjtu.iron.idempotent.api.repository.IdempotencySuccessRequest;
import com.xjtu.iron.idempotent.api.repository.IdempotencyWriteResult;
import com.xjtu.iron.idempotent.api.repository.IdempotencyWriteStatus;
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
    void concurrentOwnerSeesProcessing() {
        Instant now = Instant.now();

        IdempotencyAcquireResult first = repository.tryAcquire(request(
                "k", "h", "A", now, Duration.ofSeconds(30), true, true));
        IdempotencyAcquireResult second = repository.tryAcquire(request(
                "k", "h", "B", now.plusMillis(1), Duration.ofSeconds(30), true, true));

        assertThat(first.getStatus()).isEqualTo(IdempotencyAcquireStatus.ACQUIRED);
        assertThat(second.getStatus()).isEqualTo(IdempotencyAcquireStatus.PROCESSING);
        assertThat(second.getRecord().getOwnerToken()).isEqualTo("A");
    }

    @Test
    void expiredProcessingCanBeTakenOverAndVersionIncrements() {
        Instant now = Instant.now();
        repository.tryAcquire(request(
                "k", "h", "A", now, Duration.ofMillis(10), true, true));

        IdempotencyAcquireResult result = repository.tryAcquire(request(
                "k", "h", "B", now.plusSeconds(1), Duration.ofSeconds(30), true, true));

        assertThat(result.getStatus()).isEqualTo(IdempotencyAcquireStatus.ACQUIRED);
        assertThat(result.isTakeover()).isTrue();
        assertThat(result.getRecord().getOwnerToken()).isEqualTo("B");
        assertThat(result.getRecord().getVersion()).isEqualTo(2);
    }

    @Test
    void expiredProcessingCanStopAtFailedWhenRetryDisabled() {
        Instant now = Instant.now();
        repository.tryAcquire(request(
                "k", "h", "A", now, Duration.ofMillis(10), false, true));

        IdempotencyAcquireResult result = repository.tryAcquire(request(
                "k", "h", "B", now.plusSeconds(1), Duration.ofSeconds(30), false, true));

        assertThat(result.getStatus()).isEqualTo(IdempotencyAcquireStatus.FAILED);
        assertThat(result.getRecord().getStatus()).isEqualTo(IdempotencyStatus.FAILED);
        assertThat(result.getRecord().getFailureCode()).isEqualTo("PROCESSING_TIMEOUT");
        assertThat(result.getRecord().isFailureRetryable()).isTrue();

        // 后续请求仍然配置 retryOnProcessingTimeout=false 时，不能被通用 retryFailed 偷偷重新打开。
        IdempotencyAcquireResult again = repository.tryAcquire(request(
                "k", "h", "C", now.plusSeconds(2), Duration.ofSeconds(30), false, true));
        assertThat(again.getStatus()).isEqualTo(IdempotencyAcquireStatus.FAILED);
        assertThat(again.getRecord().getVersion()).isEqualTo(1);
    }

    @Test
    void retryableFailedRecordCanBeReacquired() {
        Instant now = Instant.now();
        IdempotencyAcquireResult first = repository.tryAcquire(request(
                "k", "h", "A", now, Duration.ofSeconds(30), true, true));

        IdempotencyWriteResult failed = repository.markFailed(new IdempotencyFailureRequest(
                "n",
                "k",
                "A",
                first.getRecord().getVersion(),
                new IdempotencyFailureInfo("TEMP", "temporary failure", true, now.plusSeconds(1)),
                null,
                now.plusSeconds(1)));
        assertThat(failed.getStatus()).isEqualTo(IdempotencyWriteStatus.UPDATED);

        IdempotencyAcquireResult second = repository.tryAcquire(request(
                "k", "h", "B", now.plusSeconds(2), Duration.ofSeconds(30), true, true));

        assertThat(second.getStatus()).isEqualTo(IdempotencyAcquireStatus.ACQUIRED);
        assertThat(second.getRecord().getVersion()).isEqualTo(2);
        assertThat(second.getRecord().getOwnerToken()).isEqualTo("B");
    }

    @Test
    void nonRetryableFailedRecordIsReturnedAsFailed() {
        Instant now = Instant.now();
        IdempotencyAcquireResult first = repository.tryAcquire(request(
                "k", "h", "A", now, Duration.ofSeconds(30), true, true));

        repository.markFailed(new IdempotencyFailureRequest(
                "n",
                "k",
                "A",
                first.getRecord().getVersion(),
                new IdempotencyFailureInfo("BIZ", "permanent", false, now.plusSeconds(1)),
                null,
                now.plusSeconds(1)));

        IdempotencyAcquireResult second = repository.tryAcquire(request(
                "k", "h", "B", now.plusSeconds(2), Duration.ofSeconds(30), true, true));

        assertThat(second.getStatus()).isEqualTo(IdempotencyAcquireStatus.FAILED);
        assertThat(second.getRecord().getOwnerToken()).isEqualTo("A");
    }

    @Test
    void staleOwnerCannotMarkSuccessAfterTakeover() {
        Instant now = Instant.now();
        IdempotencyAcquireResult first = repository.tryAcquire(request(
                "k", "h", "A", now, Duration.ofMillis(10), true, true));
        IdempotencyAcquireResult second = repository.tryAcquire(request(
                "k", "h", "B", now.plusSeconds(1), Duration.ofSeconds(30), true, true));

        IdempotencyWriteResult bSuccess = repository.markSuccess(new IdempotencySuccessRequest(
                "n", "k", "B", second.getRecord().getVersion(), "B-result", null, now.plusSeconds(2)));
        IdempotencyWriteResult aLateSuccess = repository.markSuccess(new IdempotencySuccessRequest(
                "n", "k", "A", first.getRecord().getVersion(), "A-result", null, now.plusSeconds(3)));

        assertThat(bSuccess.getStatus()).isEqualTo(IdempotencyWriteStatus.UPDATED);
        assertThat(aLateSuccess.getStatus()).isEqualTo(IdempotencyWriteStatus.ALREADY_FINAL);
        assertThat(repository.find("n", "k").orElseThrow().getResultPayload())
                .isEqualTo("B-result");
    }

    @Test
    void sameKeyWithDifferentRequestHashConflicts() {
        Instant now = Instant.now();
        repository.tryAcquire(request(
                "k", "hash-A", "A", now, Duration.ofSeconds(30), true, true));

        IdempotencyAcquireResult result = repository.tryAcquire(request(
                "k", "hash-B", "B", now.plusSeconds(1), Duration.ofSeconds(30), true, true));

        assertThat(result.getStatus()).isEqualTo(IdempotencyAcquireStatus.KEY_CONFLICT);
    }

    private IdempotencyAcquireRequest request(
            String key,
            String hash,
            String owner,
            Instant now,
            Duration processingTimeout,
            boolean retryTimeout,
            boolean retryFailed) {
        return new IdempotencyAcquireRequest(
                "n",
                key,
                hash,
                owner,
                IdempotencyMode.DURABLE,
                processingTimeout,
                null,
                retryTimeout,
                retryFailed,
                now);
    }
}
