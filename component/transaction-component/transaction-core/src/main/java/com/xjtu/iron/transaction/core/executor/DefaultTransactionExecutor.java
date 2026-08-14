package com.xjtu.iron.transaction.core.executor;

import com.xjtu.iron.transaction.api.context.TransactionParticipation;
import com.xjtu.iron.transaction.api.definition.TransactionOptions;
import com.xjtu.iron.transaction.api.event.TransactionEvent;
import com.xjtu.iron.transaction.api.event.TransactionEventListener;
import com.xjtu.iron.transaction.api.event.TransactionEventType;
import com.xjtu.iron.transaction.api.exception.TransactionExecutionException;
import com.xjtu.iron.transaction.api.execution.TransactionCallback;
import com.xjtu.iron.transaction.api.execution.TransactionExecutor;
import com.xjtu.iron.transaction.api.status.TransactionOutcome;
import com.xjtu.iron.transaction.api.status.TransactionStage;
import com.xjtu.iron.transaction.core.context.DefaultTransactionContext;
import com.xjtu.iron.transaction.core.validation.TransactionOptionsValidator;
import com.xjtu.iron.transaction.spi.exception.ProviderTransactionException;
import com.xjtu.iron.transaction.spi.provider.TransactionProvider;
import com.xjtu.iron.transaction.spi.provider.TransactionProviderResult;

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
 * <p>core 负责稳定语义、上下文和异常转换；物理事务 begin/commit/rollback 由 Provider 完成。</p>
 */
public final class DefaultTransactionExecutor implements TransactionExecutor {

    private final TransactionProvider provider;
    private final List<TransactionEventListener> listeners;

    public DefaultTransactionExecutor(TransactionProvider provider) {
        this(provider, Collections.emptyList());
    }

    public DefaultTransactionExecutor(TransactionProvider provider, List<TransactionEventListener> listeners) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.listeners = Collections.unmodifiableList(new ArrayList<>(
                listeners == null ? Collections.emptyList() : listeners));
    }

    @Override
    public <T> T execute(TransactionOptions options, TransactionCallback<T> callback) {
        // 1. 先校验 callback 和事务参数；非法参数不应该触碰数据库连接或事务管理器。
        Objects.requireNonNull(callback, "callback");
        TransactionOptionsValidator.validate(options);

        // 2. executionId 只标识本次组件调用，用于日志/事件关联，不伪装成数据库 transaction id。
        String executionId = UUID.randomUUID().toString();
        long startedNanos = System.nanoTime();

        // 3. Provider 进入事务以后才能知道本次调用是 OWNER 还是 PARTICIPANT，因此先准备运行时引用。
        AtomicReference<TransactionParticipation> participationRef = new AtomicReference<>();

        // 4. 发布开始事件。监听器只是观测能力，后续 publish 会隔离监听器自身异常。
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
            // 5. 交给 Provider 真正进入事务边界；Provider 会处理 REQUIRED / REQUIRES_NEW / MANDATORY。
            TransactionProviderResult<T> result = provider.execute(options, providerContext -> {
                // 5.1 记录真实 participation，并把 Provider 上下文包装成稳定 API TransactionContext。
                participationRef.set(providerContext.participation());
                DefaultTransactionContext context = new DefaultTransactionContext(executionId, options.name(), providerContext);

                // 5.2 在已经存在的事务边界内执行调用方业务代码。
                return callback.execute(context);
            });

            // 6. Provider 正常返回说明当前逻辑事务已完成；outcome 可能是 COMMITTED，也可能只是 PARTICIPATED。
            publish(new TransactionEvent(
                    TransactionEventType.COMPLETED,
                    executionId,
                    options.name(),
                    TransactionStage.COMPLETION,
                    result.participation(),
                    result.outcome(),
                    elapsed(startedNanos),
                    null));

            // 7. 事务组件不改变业务返回值，直接返回 callback 的结果。
            return result.value();
        } catch (ProviderTransactionException infrastructureFailure) {
            // 8. Provider 异常属于事务基础设施问题，转换成稳定的 API 异常并保留阶段/outcome。
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

            // 9. rollback 自身失败等附加信息可能以 suppressed exception 存在，必须继续保留诊断链。
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
            // 10. callback 抛出的业务异常保持原类型；Provider 已经负责 rollback 或标记外层 rollback-only。
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

            // 11. 不用统一事务异常覆盖业务异常，调用方仍可按原业务异常类型进行处理。
            throw businessFailure;
        }
    }

    private Duration elapsed(long startedNanos) {
        // 使用 monotonic nanoTime 计算耗时，避免系统时间调整导致 Duration 变成负数。
        return Duration.ofNanos(Math.max(0L, System.nanoTime() - startedNanos));
    }

    private void publish(TransactionEvent event) {
        // 观测能力必须旁路化：一个 Metrics/Logging Listener 出错不能导致业务事务失败。
        for (TransactionEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (RuntimeException ignored) {
                // 一期只做故障隔离；二期接统一 observability 后再记录 listener 自身失败指标。
            }
        }
    }
}
