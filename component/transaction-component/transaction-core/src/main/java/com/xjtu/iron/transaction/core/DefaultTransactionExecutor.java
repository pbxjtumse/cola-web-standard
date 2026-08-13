package com.xjtu.iron.transaction.core;

import com.xjtu.iron.transaction.api.*;
import com.xjtu.iron.transaction.spi.ProviderTransactionException;
import com.xjtu.iron.transaction.spi.TransactionProvider;
import com.xjtu.iron.transaction.spi.TransactionProviderResult;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TransactionExecutor 默认实现。
 *
 * <p>职责：</p>
 * <ul>
 *     <li>校验稳定 API 参数；</li>
 *     <li>生成本次逻辑执行 ID；</li>
 *     <li>把 API callback 转换为 Provider callback；</li>
 *     <li>隔离 Provider 类型，统一基础设施异常；</li>
 *     <li>发布最小生命周期事件。</li>
 * </ul>
 *
 * <p>真正的物理事务 begin/commit/rollback 不在这里实现，避免 core 重新发明事务管理器。</p>
 */
public final class DefaultTransactionExecutor implements TransactionExecutor {

    private final TransactionProvider provider;
    private final List<TransactionEventListener> listeners;

    public DefaultTransactionExecutor(TransactionProvider provider) {
        this(provider, Collections.emptyList());
    }

    public DefaultTransactionExecutor(
            TransactionProvider provider,
            List<TransactionEventListener> listeners) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.listeners = Collections.unmodifiableList(new ArrayList<>(
                listeners == null ? Collections.emptyList() : listeners));
    }

    @Override
    public <T> T execute(TransactionOptions options, TransactionCallback<T> callback) {
        Objects.requireNonNull(callback, "callback");
        TransactionOptionsValidator.validate(options);

        String executionId = UUID.randomUUID().toString();
        long startedNanos = System.nanoTime();
        AtomicReference<TransactionParticipation> participationRef = new AtomicReference<>();

        publish(new TransactionEvent(
                TransactionEventType.STARTED,
                executionId,
                options.name(),
                TransactionStage.BEGIN,
                null,
                null,
                Duration.ZERO,
                null));

        try {
            TransactionProviderResult<T> result = provider.execute(options, providerContext -> {
                participationRef.set(providerContext.participation());
                DefaultTransactionContext context = new DefaultTransactionContext(
                        executionId,
                        options.name(),
                        providerContext);
                return callback.execute(context);
            });

            publish(new TransactionEvent(
                    TransactionEventType.COMPLETED,
                    executionId,
                    options.name(),
                    TransactionStage.COMPLETION,
                    result.participation(),
                    result.outcome(),
                    elapsed(startedNanos),
                    null));

            return result.value();
        } catch (ProviderTransactionException infrastructureFailure) {
            TransactionExecutionException apiFailure = new TransactionExecutionException(
                    "Transaction infrastructure failed at stage " + infrastructureFailure.stage()
                            + ", transaction=" + options.name(),
                    executionId,
                    options.name(),
                    infrastructureFailure.stage(),
                    infrastructureFailure.outcome(),
                    infrastructureFailure.getCause() == null
                            ? infrastructureFailure
                            : infrastructureFailure.getCause());

            // Provider 可能把 rollback 失败信息以 suppressed 形式附带在异常上，继续保留。
            for (Throwable suppressed : infrastructureFailure.getSuppressed()) {
                apiFailure.addSuppressed(suppressed);
            }

            publish(new TransactionEvent(
                    TransactionEventType.INFRASTRUCTURE_FAILED,
                    executionId,
                    options.name(),
                    infrastructureFailure.stage(),
                    participationRef.get(),
                    infrastructureFailure.outcome(),
                    elapsed(startedNanos),
                    apiFailure));

            throw apiFailure;
        } catch (RuntimeException | Error businessFailure) {
            TransactionParticipation participation = participationRef.get();
            TransactionOutcome outcome = participation == TransactionParticipation.PARTICIPANT
                    ? TransactionOutcome.ROLLBACK_ONLY
                    : TransactionOutcome.ROLLED_BACK;

            publish(new TransactionEvent(
                    TransactionEventType.BUSINESS_FAILED,
                    executionId,
                    options.name(),
                    TransactionStage.EXECUTE,
                    participation,
                    outcome,
                    elapsed(startedNanos),
                    businessFailure));

            // 业务异常保持原类型和原对象继续抛出，不做统一包装。
            throw businessFailure;
        }
    }

    private Duration elapsed(long startedNanos) {
        return Duration.ofNanos(Math.max(0L, System.nanoTime() - startedNanos));
    }

    /**
     * 可观测性扩展绝不能反向改变事务结果。
     */
    private void publish(TransactionEvent event) {
        for (TransactionEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (RuntimeException ignored) {
                // 一期明确隔离监听器异常；二期接入统一 observability 后记录内部指标/日志。
            }
        }
    }
}
