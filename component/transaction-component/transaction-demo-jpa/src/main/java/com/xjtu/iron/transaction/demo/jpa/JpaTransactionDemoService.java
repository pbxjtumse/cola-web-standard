package com.xjtu.iron.transaction.demo.jpa;

import com.xjtu.iron.transaction.api.TransactionExecutor;
import com.xjtu.iron.transaction.api.TransactionOptions;
import com.xjtu.iron.transaction.api.TransactionPropagation;
import org.springframework.stereotype.Service;

/**
 * 证明 JPA 不需要 transaction-provider-jpa：
 * Repository/EntityManager 直接参与 JpaTransactionManager 管理的当前事务。
 */
@Service
public class JpaTransactionDemoService {

    private final TransactionExecutor transactionExecutor;
    private final DemoRecordRepository repository;

    public JpaTransactionDemoService(
            TransactionExecutor transactionExecutor,
            DemoRecordRepository repository) {
        this.transactionExecutor = transactionExecutor;
        this.repository = repository;
    }

    public void commit(long id) {
        transactionExecutor.executeWithoutResult(ctx ->
                repository.save(new DemoRecordEntity(id, "commit")));
    }

    public void rollback(long id) {
        transactionExecutor.executeWithoutResult(ctx -> {
            repository.save(new DemoRecordEntity(id, "rollback"));
            throw new IllegalStateException("demo rollback");
        });
    }

    public void outerRollbackInnerRequiresNew() {
        TransactionOptions innerOptions = TransactionOptions.builder()
                .name("jpa-inner-requires-new")
                .propagation(TransactionPropagation.REQUIRES_NEW)
                .build();

        transactionExecutor.executeWithoutResult(
                TransactionOptions.builder().name("jpa-outer").build(),
                outer -> {
                    repository.save(new DemoRecordEntity(2001L, "outer"));
                    transactionExecutor.executeWithoutResult(
                            innerOptions,
                            inner -> repository.save(new DemoRecordEntity(2002L, "inner")));
                    throw new IllegalStateException("rollback outer only");
                });
    }
}
