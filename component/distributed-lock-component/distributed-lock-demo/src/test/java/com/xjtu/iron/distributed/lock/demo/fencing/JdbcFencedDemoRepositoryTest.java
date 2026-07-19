package com.xjtu.iron.distributed.lock.demo.fencing;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcFencedDemoRepositoryTest {

    @Test
    void newerTokenShouldWinAndStaleTokenShouldBeRejected() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:fenced_resource;MODE=MySQL;DB_CLOSE_DELAY=-1");
        JdbcFencedDemoRepository repository = new JdbcFencedDemoRepository(new JdbcTemplate(dataSource));
        repository.initializeSchema();

        assertThat(repository.updateIfNewer("order:1", "token-10", 10L)).isTrue();
        assertThat(repository.updateIfNewer("order:1", "token-12", 12L)).isTrue();
        assertThat(repository.updateIfNewer("order:1", "stale-token-11", 11L)).isFalse();
        assertThat(repository.currentToken("order:1")).isEqualTo(12L);
        assertThat(repository.currentPayload("order:1")).isEqualTo("token-12");
    }
}
