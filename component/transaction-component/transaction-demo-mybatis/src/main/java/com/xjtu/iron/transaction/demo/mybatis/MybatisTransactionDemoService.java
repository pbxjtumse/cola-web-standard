package com.xjtu.iron.transaction.demo.mybatis;

import com.xjtu.iron.transaction.api.TransactionExecutor;
import com.xjtu.iron.transaction.api.TransactionOptions;
import com.xjtu.iron.transaction.api.TransactionPropagation;
import org.springframework.stereotype.Service;

/**
 * 证明 MyBatis 不需要任何 transaction-provider-mybatis：
 * Mapper 直接参与当前 Spring 管理的 JDBC 事务。
 */
@Service
public class MybatisTransactionDemoService {

    private final TransactionExecutor transactionExecutor;
    private final DemoRecordMapper mapper;

    public MybatisTransactionDemoService(
            TransactionExecutor transactionExecutor,
            DemoRecordMapper mapper) {
        this.transactionExecutor = transactionExecutor;
        this.mapper = mapper;
    }

    public void commit(long id) {
        transactionExecutor.executeWithoutResult(ctx -> mapper.insert(id, "commit"));
    }

    public void rollback(long id) {
        transactionExecutor.executeWithoutResult(ctx -> {
            mapper.insert(id, "rollback");
            throw new IllegalStateException("demo rollback");
        });
    }

    public void outerRollbackInnerRequiresNew() {
        TransactionOptions innerOptions = TransactionOptions.builder()
                .name("mybatis-inner-requires-new")
                .propagation(TransactionPropagation.REQUIRES_NEW)
                .build();

        transactionExecutor.executeWithoutResult(
                TransactionOptions.builder().name("mybatis-outer").build(),
                outer -> {
                    mapper.insert(1001L, "outer");
                    transactionExecutor.executeWithoutResult(
                            innerOptions,
                            inner -> mapper.insert(1002L, "inner"));
                    throw new IllegalStateException("rollback outer only");
                });
    }
}
