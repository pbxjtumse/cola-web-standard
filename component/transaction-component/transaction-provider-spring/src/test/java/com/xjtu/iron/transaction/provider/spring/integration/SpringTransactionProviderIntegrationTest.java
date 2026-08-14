package com.xjtu.iron.transaction.provider.spring.integration;

import com.xjtu.iron.transaction.api.definition.TransactionOptions;
import com.xjtu.iron.transaction.api.definition.TransactionPropagation;
import com.xjtu.iron.transaction.api.execution.TransactionExecutor;
import com.xjtu.iron.transaction.core.executor.DefaultTransactionExecutor;
import com.xjtu.iron.transaction.provider.spring.transaction.SpringTransactionProvider;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringTransactionProviderIntegrationTest {

    private JdbcTemplate jdbc;
    private TransactionExecutor executor;

    @BeforeEach
    void setUp() {
        // 1. 测试使用独立 H2 DataSource，不依赖外部数据库环境。
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:tx_provider;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");

        // 2. JdbcTemplate 和 DataSourceTransactionManager 必须共享同一个 DataSource 才能参与同一事务。
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("drop table if exists tx_demo");
        jdbc.execute("create table tx_demo(id int primary key, name varchar(64))");

        // 3. 手工组装完整链路：Executor -> SpringTransactionProvider -> DataSourceTransactionManager。
        executor = new DefaultTransactionExecutor(
                new SpringTransactionProvider(new DataSourceTransactionManager(dataSource)));
    }

    @Test
    void shouldCommitSuccessfulTransaction() {
        // callback 正常结束，OWNER 事务应被物理提交。
        executor.executeWithoutResult(ctx -> jdbc.update(
                "insert into tx_demo(id, name) values (?, ?)", 1, "committed"));

        assertEquals(1, count());
    }

    @Test
    void shouldRollbackAndKeepOriginalBusinessException() {
        // 业务异常发生在 INSERT 以后，Provider 应 rollback 并把同一个业务异常继续抛出。
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
        // 1. 准备内层 REQUIRES_NEW 配置。
        TransactionOptions requiresNew = TransactionOptions.builder()
                .name("inner-new")
                .propagation(TransactionPropagation.REQUIRES_NEW)
                .build();

        // 2. outer 写 1；inner 独立 Tx-B 写 2 并提交；随后 outer Tx-A 故意回滚。
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

        // 3. outer 数据消失，inner 数据保留。
        assertEquals(1, count());
        assertEquals("inner", jdbc.queryForObject(
                "select name from tx_demo where id = 2", String.class));
    }

    @Test
    void requiredInnerShouldParticipateInOuterTransaction() {
        // outer 创建新事务；inner REQUIRED 应识别为 PARTICIPANT，而不是再次创建物理事务。
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
        // callback 不抛异常，但主动将事务标记 rollback-only。
        executor.executeWithoutResult(ctx -> {
            jdbc.update("insert into tx_demo(id, name) values (?, ?)", 1, "rollback-only");
            ctx.setRollbackOnly();
        });

        // 最终应由 Spring 在完成阶段回滚。
        assertEquals(0, count());
    }

    private int count() {
        // 辅助查询只用于断言数据库最终状态，不参与被测事务。
        Integer count = jdbc.queryForObject("select count(*) from tx_demo", Integer.class);
        return count == null ? 0 : count;
    }
}
