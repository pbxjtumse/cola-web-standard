package com.xjtu.iron.transaction.provider.spring.transaction;

import com.xjtu.iron.transaction.api.context.TransactionParticipation;
import com.xjtu.iron.transaction.api.definition.TransactionOptions;
import com.xjtu.iron.transaction.api.status.TransactionOutcome;
import com.xjtu.iron.transaction.api.status.TransactionStage;
import com.xjtu.iron.transaction.provider.spring.context.SpringProviderTransactionContext;
import com.xjtu.iron.transaction.provider.spring.mapping.SpringTransactionDefinitionMapper;
import com.xjtu.iron.transaction.spi.exception.ProviderTransactionException;
import com.xjtu.iron.transaction.spi.provider.TransactionProvider;
import com.xjtu.iron.transaction.spi.provider.TransactionProviderCallback;
import com.xjtu.iron.transaction.spi.provider.TransactionProviderResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.Objects;

/**
 * 基于 Spring PlatformTransactionManager 的本地事务 Provider。
 *
 * <p>MyBatis/JDBC、JPA/Hibernate 的差异由 Spring 具体 TransactionManager 负责，
 * 本 Provider 只依赖统一的 PlatformTransactionManager。</p>
 */
public final class SpringTransactionProvider implements TransactionProvider {

    private final PlatformTransactionManager transactionManager;

    public SpringTransactionProvider(PlatformTransactionManager transactionManager) {
        this.transactionManager = Objects.requireNonNull(transactionManager, "transactionManager");
    }

    @Override
    public <T> TransactionProviderResult<T> execute(TransactionOptions options, TransactionProviderCallback<T> callback) {

        // 1. Provider 层再次守住边界参数，避免直接使用 SPI 时得到难以定位的空指针。
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(callback, "callback");

        final DefaultTransactionDefinition springDefinition;
        try {
            // 2. 将稳定的组件配置翻译为 Spring TransactionDefinition。
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
            // 3. 真正向 Spring 请求事务：
            // REQUIRED 可能创建新事务或加入已有事务；
            // REQUIRES_NEW 的挂起/恢复由 PlatformTransactionManager 自己负责；
            // MANDATORY 在没有外层事务时会在这里失败。
            status = transactionManager.getTransaction(springDefinition);
        } catch (org.springframework.transaction.TransactionException beginFailure) {
            throw new ProviderTransactionException(
                    "Failed to begin or join transaction " + options.name(),
                    TransactionStage.BEGIN,
                    TransactionOutcome.FAILED,
                    beginFailure);
        }

        // 4. Spring 已经告诉我们本次逻辑事务是否真正新建物理事务，据此确定 OWNER/PARTICIPANT。
        TransactionParticipation participation = status.isNewTransaction()
                ? TransactionParticipation.OWNER
                : TransactionParticipation.PARTICIPANT;

        SpringProviderTransactionContext providerContext =
                new SpringProviderTransactionContext(status, participation);

        final T value;
        try {
            // 5. 只有事务边界建立完成后才执行真正业务代码，MyBatis/JPA 会参与当前 Spring 事务。
            value = callback.execute(providerContext);
        } catch (RuntimeException | Error businessFailure) {
            // 6. 业务异常先回滚（或把外层事务标记 rollback-only），再保持原业务异常向上抛出。
            rollbackAfterBusinessFailure(status, businessFailure, options.name());
            throw businessFailure;
        }

        // 7. callback 正常结束也可能主动 setRollbackOnly，因此提交前先记录状态用于确定 outcome。
        boolean rollbackOnlyBeforeCompletion = status.isRollbackOnly();

        try {
            // 8. OWNER 时 commit 会完成物理提交；PARTICIPANT 时 commit 只完成当前逻辑事务，不能声称外层已提交。
            transactionManager.commit(status);
        } catch (UnexpectedRollbackException unexpectedRollback) {
            // 9. Spring 明确告诉我们最终发生 rollback，因此是 ROLLED_BACK，而不是 COMMIT_UNKNOWN。
            throw new ProviderTransactionException(
                    "Transaction rolled back unexpectedly during commit: " + options.name(),
                    TransactionStage.COMMIT,
                    TransactionOutcome.ROLLED_BACK,
                    unexpectedRollback);
        } catch (org.springframework.transaction.TransactionException commitFailure) {
            // 10. 一般 commit 基础设施异常可能存在“数据库已提交但客户端没收到确认”的不确定窗口。
            // 上层幂等/重试逻辑不能把这个状态直接当成“肯定没提交”。
            throw new ProviderTransactionException(
                    "Transaction commit result is unknown: " + options.name(),
                    TransactionStage.COMMIT,
                    TransactionOutcome.COMMIT_UNKNOWN,
                    commitFailure);
        }

        // 11. 当前逻辑事务正常完成，根据 participation 和 rollback-only 状态产生稳定结果。
        TransactionOutcome outcome = determineOutcome(participation, rollbackOnlyBeforeCompletion);
        return new TransactionProviderResult<>(value, participation, outcome);
    }

    private TransactionOutcome determineOutcome(
            TransactionParticipation participation,
            boolean rollbackOnlyBeforeCompletion) {

        // PARTICIPANT 永远不能返回 COMMITTED，因为真正的物理提交仍由外层事务控制。
        if (participation == TransactionParticipation.PARTICIPANT) {
            return rollbackOnlyBeforeCompletion
                    ? TransactionOutcome.ROLLBACK_ONLY
                    : TransactionOutcome.PARTICIPATED;
        }

        // OWNER 才能确认本次新建事务最终提交或因为 rollback-only 被回滚。
        return rollbackOnlyBeforeCompletion
                ? TransactionOutcome.ROLLED_BACK
                : TransactionOutcome.COMMITTED;
    }

    private void rollbackAfterBusinessFailure(
            TransactionStatus status,
            Throwable businessFailure,
            String transactionName) {
        try {
            // OWNER 通常执行物理 rollback；PARTICIPANT 则由 Spring 将外层事务标记为 rollback-only。
            transactionManager.rollback(status);
        } catch (org.springframework.transaction.TransactionException rollbackFailure) {
            // rollback 失败是第二故障，不能覆盖最初导致事务失败的业务异常。
            ProviderTransactionException rollbackInfrastructureFailure =
                    new ProviderTransactionException(
                            "Rollback failed after business failure: " + transactionName,
                            TransactionStage.ROLLBACK,
                            TransactionOutcome.FAILED,
                            rollbackFailure);

            // 用 suppressed 保留 rollback 基础设施故障，最终调用方仍首先看到原始业务异常。
            businessFailure.addSuppressed(rollbackInfrastructureFailure);
        }
    }
}
