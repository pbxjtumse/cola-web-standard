package com.xjtu.iron.transaction.provider.spring;

import com.xjtu.iron.transaction.api.*;
import com.xjtu.iron.transaction.core.DefaultTransactionExecutor;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import static org.junit.jupiter.api.Assertions.*;

class SpringTransactionProviderIntegrationTest {

    private JdbcTemplate jdbc;
    private TransactionExecutor executor;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:tx_provider;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");

        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("drop table if exists tx_demo");
        jdbc.execute("create table tx_demo(id int primary key, name varchar(64))");

        executor = new DefaultTransactionExecutor(
                new SpringTransactionProvider(new DataSourceTransactionManager(dataSource)));
    }

    @Test
    void shouldCommitSuccessfulTransaction() {
        executor.executeWithoutResult(ctx -> jdbc.update(
                "insert into tx_demo(id, name) values (?, ?)", 1, "committed"));

        assertEquals(1, count());
    }

    @Test
    void shouldRollbackAndKeepOriginalBusinessException() {
        IllegalStateException failure = assertThrows(IllegalStateException.class, () ->
                executor.executeWithoutResult(ctx -> {
                    jdbc.update("insert into tx_demo(id, name) values (?, ?)", 1, "rollback");
                    throw new IllegalStateException("business failed");
                }));

        assertEquals("business failed", failure.getMessage());
        assertEquals(0, count());
    }

    @Test
    void requiresNewShouldSurviveOuterRollback() {
        TransactionOptions requiresNew = TransactionOptions.builder()
                .name("inner-new")
                .propagation(TransactionPropagation.REQUIRES_NEW)
                .build();

        assertThrows(IllegalStateException.class, () ->
                executor.executeWithoutResult(
                        TransactionOptions.builder().name("outer").build(),
                        outer -> {
                            jdbc.update("insert into tx_demo(id, name) values (?, ?)", 1, "outer");

                            executor.executeWithoutResult(requiresNew, inner -> {
                                assertTrue(inner.isNewTransaction());
                                jdbc.update("insert into tx_demo(id, name) values (?, ?)", 2, "inner");
                            });

                            throw new IllegalStateException("rollback outer");
                        }));

        assertEquals(1, count());
        assertEquals("inner", jdbc.queryForObject(
                "select name from tx_demo where id = 2", String.class));
    }

    @Test
    void requiredInnerShouldParticipateInOuterTransaction() {
        executor.executeWithoutResult(
                TransactionOptions.builder().name("outer").build(),
                outer -> {
                    assertTrue(outer.isNewTransaction());
                    executor.executeWithoutResult(
                            TransactionOptions.builder().name("inner-required").build(),
                            inner -> assertTrue(inner.isParticipating()));
                });
    }

    @Test
    void rollbackOnlyShouldRollbackOwnerTransaction() {
        executor.executeWithoutResult(ctx -> {
            jdbc.update("insert into tx_demo(id, name) values (?, ?)", 1, "rollback-only");
            ctx.setRollbackOnly();
        });

        assertEquals(0, count());
    }

    private int count() {
        Integer count = jdbc.queryForObject("select count(*) from tx_demo", Integer.class);
        return count == null ? 0 : count;
    }
}
