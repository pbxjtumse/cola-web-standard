package com.xjtu.iron.distributed.lock.provider.jdbc.fencing;

import com.xjtu.iron.distributed.lock.api.LockOptions;
import com.xjtu.iron.distributed.lock.core.fencing.FencingTokenRequest;
import com.xjtu.iron.distributed.lock.core.fencing.FencingTokenResponse;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcSequenceFencingTokenProviderTest {

    private JdbcSequenceFencingTokenProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:fencing;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS iron_lock_fencing_token");
            statement.execute("CREATE TABLE iron_lock_fencing_token ("
                    + "namespace VARCHAR(128) NOT NULL,"
                    + "lock_name VARCHAR(512) NOT NULL,"
                    + "current_token BIGINT NOT NULL,"
                    + "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "PRIMARY KEY(namespace, lock_name))");
        }
        provider = new JdbcSequenceFencingTokenProvider(dataSource);
    }

    @Test
    void shouldIssueIncreasingTokensForSameLock() {
        long first = provider.nextToken(request("order:1")).token().orElseThrow();
        long second = provider.nextToken(request("order:1")).token().orElseThrow();
        long anotherLock = provider.nextToken(request("order:2")).token().orElseThrow();

        assertThat(first).isEqualTo(1L);
        assertThat(second).isEqualTo(2L);
        assertThat(anotherLock).isEqualTo(1L);
    }

    @Test
    void concurrentIssuanceShouldBeUniqueAndMonotonic() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Long>> tasks = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                tasks.add(() -> {
                    FencingTokenResponse response = provider.nextToken(request("concurrent"));
                    assertThat(response.isIssued()).isTrue();
                    return response.token().orElseThrow();
                });
            }
            List<Future<Long>> futures = executor.invokeAll(tasks);
            List<Long> tokens = new ArrayList<>();
            for (Future<Long> future : futures) {
                tokens.add(future.get());
            }
            assertThat(tokens).doesNotHaveDuplicates();
            assertThat(tokens).containsExactlyInAnyOrderElementsOf(
                    java.util.stream.LongStream.rangeClosed(1, 20).boxed().toList());
        } finally {
            executor.shutdownNow();
        }
    }

    private FencingTokenRequest request(String lockName) {
        return FencingTokenRequest.builder()
                .namespace("test")
                .lockName(lockName)
                .ownerToken("owner")
                .options(LockOptions.builder().fencingRequired(true).build())
                .build();
    }
}
