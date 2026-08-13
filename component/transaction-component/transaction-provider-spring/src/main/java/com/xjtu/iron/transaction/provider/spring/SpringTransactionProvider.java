package com.xjtu.iron.transaction.provider.spring;

import com.xjtu.iron.transaction.api.*;
import com.xjtu.iron.transaction.spi.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.Objects;

/**
 * 基于 Spring PlatformTransactionManager 的本地事务 Provider。
 *
 * <p>MyBatis/JDBC 通常由 JdbcTransactionManager/DataSourceTransactionManager 提供事务；
 * JPA/Hibernate 通常由 JpaTransactionManager 提供事务。本 Provider 只依赖统一的
 * PlatformTransactionManager，因此不需要 MyBatisProvider、JpaProvider 等 ORM 专属适配层。</p>
 */
public final class SpringTransactionProvider implements TransactionProvider {

    private final PlatformTransactionManager transactionManager;

    public SpringTransactionProvider(PlatformTransactionManager transactionManager) {
        this.transactionManager = Objects.requireNonNull(transactionManager, "transactionManager");
    }

    @Override
    public <T> TransactionProviderResult<T> execute(
            TransactionOptions options,
            TransactionProviderCallback<T> callback) {

        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(callback, "callback");

        final DefaultTransactionDefinition springDefinition;
        try {
            springDefinition = SpringTransactionDefinitionMapper.map(options);
        } catch (RuntimeException mappingFailure) {
            throw new ProviderTransactionException(
                    "Failed to resolve Spring transaction definition " + options.name(),
                    TransactionStage.RESOLVE,
                    TransactionOutcome.FAILED,
                    mappingFailure);
        }

        final TransactionStatus status;
        try {
            // REQUIRED: 可能创建新事务，也可能返回“参与已有事务”的逻辑状态。
            // REQUIRES_NEW: 挂起/恢复外层事务由 PlatformTransactionManager 自己负责。
            // MANDATORY: 若当前无事务，Spring 会在这里拒绝。
            status = transactionManager.getTransaction(springDefinition);
        } catch (org.springframework.transaction.TransactionException beginFailure) {
            throw new ProviderTransactionException(
                    "Failed to begin or join transaction " + options.name(),
                    TransactionStage.BEGIN,
                    TransactionOutcome.FAILED,
                    beginFailure);
        }

        TransactionParticipation participation = status.isNewTransaction()
                ? TransactionParticipation.OWNER
                : TransactionParticipation.PARTICIPANT;

        SpringProviderTransactionContext providerContext =
                new SpringProviderTransactionContext(status, participation);

        final T value;
        try {
            value = callback.execute(providerContext);
        } catch (RuntimeException | Error businessFailure) {
            rollbackAfterBusinessFailure(status, businessFailure, options.name());
            // 业务异常必须保持原类型继续向上抛出。
            throw businessFailure;
        }

        boolean rollbackOnlyBeforeCompletion = status.isRollbackOnly();

        try {
            // 即使 participation=PARTICIPANT 也必须调用 commit(status)：
            // 对参与事务而言，它负责完成当前“逻辑事务”的状态处理，但不会物理提交外层事务。
            transactionManager.commit(status);
        } catch (UnexpectedRollbackException unexpectedRollback) {
            // Spring 明确告诉我们最终发生了 rollback，不属于“提交结果未知”。
            throw new ProviderTransactionException(
                    "Transaction rolled back unexpectedly during commit: " + options.name(),
                    TransactionStage.COMMIT,
                    TransactionOutcome.ROLLED_BACK,
                    unexpectedRollback);
        } catch (org.springframework.transaction.TransactionException commitFailure) {
            // 对一般 commit 基础设施异常，本地调用方通常不能仅凭异常可靠判断数据库最终状态。
            // 因此保守标记 COMMIT_UNKNOWN；上层幂等/重试逻辑不能把它简单等同于“肯定失败”。
            throw new ProviderTransactionException(
                    "Transaction commit result is unknown: " + options.name(),
                    TransactionStage.COMMIT,
                    TransactionOutcome.COMMIT_UNKNOWN,
                    commitFailure);
        }

        TransactionOutcome outcome = determineOutcome(participation, rollbackOnlyBeforeCompletion);
        return new TransactionProviderResult<>(value, participation, outcome);
    }

    private TransactionOutcome determineOutcome(
            TransactionParticipation participation,
            boolean rollbackOnlyBeforeCompletion) {

        if (participation == TransactionParticipation.PARTICIPANT) {
            return rollbackOnlyBeforeCompletion
                    ? TransactionOutcome.ROLLBACK_ONLY
                    : TransactionOutcome.PARTICIPATED;
        }

        return rollbackOnlyBeforeCompletion
                ? TransactionOutcome.ROLLED_BACK
                : TransactionOutcome.COMMITTED;
    }

    private void rollbackAfterBusinessFailure(
            TransactionStatus status,
            Throwable businessFailure,
            String transactionName) {
        try {
            // OWNER 时通常执行物理 rollback；PARTICIPANT 时通常把外层事务标记 rollback-only。
            transactionManager.rollback(status);
        } catch (org.springframework.transaction.TransactionException rollbackFailure) {
            // 原始业务异常是导致回滚的第一原因，不能被 rollback 基础设施异常覆盖。
            ProviderTransactionException rollbackInfrastructureFailure =
                    new ProviderTransactionException(
                            "Rollback failed after business failure: " + transactionName,
                            TransactionStage.ROLLBACK,
                            TransactionOutcome.FAILED,
                            rollbackFailure);
            businessFailure.addSuppressed(rollbackInfrastructureFailure);
        }
    }
}
