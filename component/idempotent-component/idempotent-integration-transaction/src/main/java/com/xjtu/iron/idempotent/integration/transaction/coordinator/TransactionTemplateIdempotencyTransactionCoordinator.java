package com.xjtu.iron.idempotent.integration.transaction.coordinator;

import com.xjtu.iron.idempotent.core.transaction.IdempotencyTransactionCoordinator;
import com.xjtu.iron.idempotent.core.transaction.IdempotencyTransactionException;
import com.xjtu.iron.idempotent.core.transaction.IdempotencyTransactionOutcome;
import com.xjtu.iron.idempotent.core.transaction.IdempotencyTransactionalWork;
import com.xjtu.iron.transaction.api.definition.TransactionOptions;
import com.xjtu.iron.transaction.api.definition.TransactionPropagation;
import com.xjtu.iron.transaction.api.exception.TransactionExecutionException;
import com.xjtu.iron.transaction.api.execution.TransactionExecutor;
import com.xjtu.iron.transaction.api.status.TransactionOutcome;

import java.util.Objects;

/**
 * 使用 transaction-component 的 {@link TransactionExecutor} 实现幂等 Tx-B。
 *
 * <p>固定使用 REQUIRED：</p>
 * <ul>
 *     <li>调用方当前没有事务：创建一个业务事务；</li>
 *     <li>调用方已经有事务：加入该事务，不自行制造第二个业务提交点。</li>
 * </ul>
 *
 * <p>这里不把 idempotencyKey / routeKey 塞进 transactionName，避免日志和指标出现高基数。</p>
 */
public final class TransactionTemplateIdempotencyTransactionCoordinator
        implements IdempotencyTransactionCoordinator {

    private final TransactionExecutor transactionExecutor;

    public TransactionTemplateIdempotencyTransactionCoordinator(TransactionExecutor transactionExecutor) {
        this.transactionExecutor = Objects.requireNonNull(transactionExecutor, "transactionExecutor must not be null");
    }

    @Override
    public <T> T executeRequired(
            String transactionName,
            String routeKey,
            IdempotencyTransactionalWork<T> work) throws Exception {
        Objects.requireNonNull(work, "work must not be null");

        TransactionOptions options = TransactionOptions.builder()
                .name(normalizeName(transactionName))
                .propagation(TransactionPropagation.REQUIRED)
                .build();

        try {
            return transactionExecutor.execute(options, context -> {
                try {
                    return work.execute();
                } catch (RuntimeException | Error unchecked) {
                    // 保留原始运行时业务异常；TransactionExecutor 会负责让当前事务回滚。
                    throw unchecked;
                } catch (Exception checked) {
                    // transaction-api 的 callback 不声明 checked exception，先临时包装；
                    // 外层在事务回滚完成以后再恢复原始异常。
                    throw new CheckedWorkRuntimeException(checked);
                }
            });
        } catch (CheckedWorkRuntimeException checked) {
            throw checked.original;
        } catch (TransactionExecutionException infrastructureFailure) {
            throw new IdempotencyTransactionException(
                    "idempotency business transaction failed at stage "
                            + infrastructureFailure.stage(),
                    infrastructureFailure.stage().name(),
                    mapOutcome(infrastructureFailure.outcome()),
                    infrastructureFailure);
        }
    }

    private String normalizeName(String transactionName) {
        return transactionName == null || transactionName.isBlank()
                ? "idempotency-business"
                : transactionName;
    }

    private IdempotencyTransactionOutcome mapOutcome(TransactionOutcome outcome) {
        if (outcome == null) {
            return IdempotencyTransactionOutcome.FAILED;
        }
        return switch (outcome) {
            case ROLLED_BACK -> IdempotencyTransactionOutcome.ROLLED_BACK;
            case COMMIT_UNKNOWN -> IdempotencyTransactionOutcome.COMMIT_UNKNOWN;
            case FAILED -> IdempotencyTransactionOutcome.FAILED;
        };
    }

    /** 只用于跨过 transaction-api 不声明 checked exception 的 callback 边界。 */
    private static final class CheckedWorkRuntimeException extends RuntimeException {
        private final Exception original;

        private CheckedWorkRuntimeException(Exception original) {
            super(original);
            this.original = original;
        }
    }
}
