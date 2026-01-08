package com.xjtu.iron.cola.web;

import com.xjtu.iron.cola.web.enums.TransactionStatusEnums;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 稳健的事务模板（语义增强版）
 *
 * 设计原则：
 * 1) execute(...) 默认对齐 Spring：异常抛出 + 事务回滚语义一致
 * 2) “异常但提交”必须显式调用 executeCommitOnException(...)，避免误用
 * 3) 异步只负责换线程：新线程新事务（不会跨线程传播）
 */
@Slf4j
public class RobustTransactionTemplate extends TransactionTemplate {

    private final Executor asyncExecutor;

    public RobustTransactionTemplate(PlatformTransactionManager transactionManager) {
        super(transactionManager);
        this.asyncExecutor = null;
    }

    public RobustTransactionTemplate(PlatformTransactionManager transactionManager, Executor asyncExecutor) {
        super(transactionManager);
        this.asyncExecutor = asyncExecutor;
    }

    public RobustTransactionTemplate(PlatformTransactionManager transactionManager, TransactionDefinition definition) {
        super(transactionManager, definition);
        this.asyncExecutor = null;
    }

    public RobustTransactionTemplate(PlatformTransactionManager transactionManager,
                                     TransactionDefinition definition,
                                     Executor asyncExecutor) {
        super(transactionManager, definition);
        this.asyncExecutor = asyncExecutor;
    }

    /* =========================================================
     * 1) 默认语义：对齐 Spring
     * ========================================================= */

    /**
     * 默认执行：遵循 Spring 语义（RuntimeException/Error 回滚；checked 默认不回滚）
     */
    public <T> T execute(Callable<T> callable) {
        return execute(callable, RollbackRule.springDefault());
    }

    /**
     * 执行并允许自定义回滚规则（仍保持：异常会抛出）
     *
     * 关键语义：如果发生异常且规则决定“回滚”，则 rollback；
     * 如果规则决定“不回滚”，则 commit；但无论如何，异常都会抛出（由调用方决定是否吞掉）。
     *
     * ⚠️ 注意：不回滚但仍抛异常，可能导致上层“误以为失败去重试”。因此不建议用它表达“异常也算成功”。
     * 如果你要“异常但算成功”，请用 executeCommitOnException(...)。
     */
    public <T> T execute(Callable<T> callable, RollbackRule rule) {
        Objects.requireNonNull(callable, "callable must not be null");
        RollbackRule rr = (rule == null) ? RollbackRule.springDefault() : rule;

        return super.execute(status -> {
            try {
                return callable.call();
            } catch (Throwable t) {
                boolean rollback = rr.rollbackOn.test(t);

                if (rollback) {
                    status.setRollbackOnly();
                    safeCallback(rr.onRollback, t, "onRollback");
                    log.debug("Transaction marked rollback-only: {}", t.toString());
                } else {
                    safeCallback(rr.onCommitDespiteException, t, "onCommitDespiteException");
                    log.debug("Transaction will COMMIT despite exception: {}", t.toString());
                }

                safeCallback(rr.onException, t, "onException");

                // 仍然抛出：对齐 execute 的“异常就是异常”的语义
                throw wrapToRuntime(t);
            }
        });
    }

    /* =========================================================
     * 2) 显式语义：异常也可能提交，但不再抛出异常给调用方
     * ========================================================= */

    /**
     * 显式“异常但提交”API：
     * - 不再把异常抛给上层（避免上层误判失败重试）
     * - 返回 TxOutcome，让调用方清楚知道：是否提交、是否异常、异常是什么
     *
     * 这才是工程上最稳的“异常但继续提交”表达方式。
     */
    public <T> TxOutcome<T> executeCommitOnException(Callable<T> callable, RollbackRule rule) {
        Objects.requireNonNull(callable, "callable must not be null");
        RollbackRule rr = (rule == null) ? RollbackRule.springDefault() : rule;

        return super.execute(status -> {
            try {
                T value = callable.call();
                return TxOutcome.committed(value);
            } catch (Throwable t) {
                boolean rollback = rr.rollbackOn.test(t);

                if (rollback) {
                    status.setRollbackOnly();
                    safeCallback(rr.onRollback, t, "onRollback");
                    safeCallback(rr.onException, t, "onException");
                    log.debug("CommitOnException: will ROLLBACK due to {}", t.toString());
                    return TxOutcome.rolledBack(t);
                } else {
                    // 不设置 rollback-only => 会提交
                    safeCallback(rr.onCommitDespiteException, t, "onCommitDespiteException");
                    safeCallback(rr.onException, t, "onException");
                    log.debug("CommitOnException: will COMMIT despite {}", t.toString());
                    return TxOutcome.committedWithException(t);
                }
            }
        });
    }

    /* =========================================================
     * 3) 异步：仅换线程，新线程新事务
     * ========================================================= */

    public <T> CompletableFuture<T> executeAsync(Callable<T> callable) {
        return executeAsync(callable, RollbackRule.springDefault());
    }

    public <T> CompletableFuture<T> executeAsync(Callable<T> callable, RollbackRule rule) {
        Assert.notNull(asyncExecutor, "Async executor must be configured for async execution");
        return CompletableFuture.supplyAsync(() -> execute(callable, rule), asyncExecutor);
    }

    public <T> CompletableFuture<TxOutcome<T>> executeCommitOnExceptionAsync(Callable<T> callable, RollbackRule rule) {
        Assert.notNull(asyncExecutor, "Async executor must be configured for async execution");
        return CompletableFuture.supplyAsync(() -> executeCommitOnException(callable, rule), asyncExecutor);
    }

    /* =========================================================
     * 4) Builder：构造独立 definition，避免污染共享模板
     * ========================================================= */

    public DefinitionBuilder withDefinition() {
        return new DefinitionBuilder(getTransactionManager(), asyncExecutor);
    }

    public static final class DefinitionBuilder {
        private final PlatformTransactionManager tm;
        private final Executor ex;
        private final DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        private RollbackRule rule = RollbackRule.springDefault();

        private DefinitionBuilder(PlatformTransactionManager tm, Executor ex) {
            this.tm = tm;
            this.ex = ex;
        }

        public DefinitionBuilder propagation(int propagationBehavior) {
            def.setPropagationBehavior(propagationBehavior);
            return this;
        }

        public DefinitionBuilder isolation(int isolationLevel) {
            def.setIsolationLevel(isolationLevel);
            return this;
        }

        public DefinitionBuilder timeout(int timeoutSeconds) {
            def.setTimeout(timeoutSeconds);
            return this;
        }

        public DefinitionBuilder readOnly(boolean readOnly) {
            def.setReadOnly(readOnly);
            return this;
        }

        public DefinitionBuilder name(String name) {
            def.setName(name);
            return this;
        }

        public DefinitionBuilder rollbackRule(RollbackRule rule) {
            if (rule != null) this.rule = rule;
            return this;
        }

        public RobustTransactionTemplate build() {
            return new RobustTransactionTemplate(tm, def, ex);
        }

        public <T> T execute(Callable<T> callable) {
            return build().execute(callable, rule);
        }

        public <T> TxOutcome<T> executeCommitOnException(Callable<T> callable) {
            return build().executeCommitOnException(callable, rule);
        }

        public <T> CompletableFuture<T> executeAsync(Callable<T> callable) {
            RobustTransactionTemplate t = build();
            return t.executeAsync(callable, rule);
        }

        public <T> CompletableFuture<TxOutcome<T>> executeCommitOnExceptionAsync(Callable<T> callable) {
            RobustTransactionTemplate t = build();
            return t.executeCommitOnExceptionAsync(callable, rule);
        }
    }

    /* =========================================================
     * 5) RollbackRule：更贴近 Spring + 更可控
     * ========================================================= */

    public static final class RollbackRule {
        private final Predicate<Throwable> rollbackOn;
        private final Consumer<Throwable> onException;
        private final Consumer<Throwable> onRollback;
        private final Consumer<Throwable> onCommitDespiteException;

        private RollbackRule(Predicate<Throwable> rollbackOn,
                             Consumer<Throwable> onException,
                             Consumer<Throwable> onRollback,
                             Consumer<Throwable> onCommitDespiteException) {
            this.rollbackOn = rollbackOn;
            this.onException = onException;
            this.onRollback = onRollback;
            this.onCommitDespiteException = onCommitDespiteException;
        }

        /**
         * Spring 默认：RuntimeException / Error 回滚
         */
        public static RollbackRule springDefault() {
            return new RollbackRule(
                    t -> (t instanceof RuntimeException) || (t instanceof Error),
                    t -> {},
                    t -> {},
                    t -> {}
            );
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private Predicate<Throwable> rollbackOn = t -> (t instanceof RuntimeException) || (t instanceof Error);
            private Consumer<Throwable> onException = t -> {};
            private Consumer<Throwable> onRollback = t -> {};
            private Consumer<Throwable> onCommitDespiteException = t -> {};

            public Builder rollbackWhen(Predicate<Throwable> predicate) {
                this.rollbackOn = Objects.requireNonNull(predicate);
                return this;
            }

            public Builder onException(Consumer<Throwable> c) {
                this.onException = Objects.requireNonNull(c);
                return this;
            }

            public Builder onRollback(Consumer<Throwable> c) {
                this.onRollback = Objects.requireNonNull(c);
                return this;
            }

            public Builder onCommitDespiteException(Consumer<Throwable> c) {
                this.onCommitDespiteException = Objects.requireNonNull(c);
                return this;
            }

            public RollbackRule build() {
                return new RollbackRule(rollbackOn, onException, onRollback, onCommitDespiteException);
            }
        }
    }

    /* =========================================================
     * 6) TxOutcome：显式表达“提交/回滚/提交但异常”
     * ========================================================= */

    public static final class TxOutcome<T> {
        private final TransactionStatusEnums transactionStatusEnums;
        private final T value;
        private final Throwable exception;

        private TxOutcome(TransactionStatusEnums transactionStatusEnums, T value, Throwable exception) {
            this.transactionStatusEnums = transactionStatusEnums;
            this.value = value;
            this.exception = exception;
        }

        public static <T> TxOutcome<T> committed(T value) {
            return new TxOutcome<>(TransactionStatusEnums.COMMITTED, value, null);
        }

        public static <T> TxOutcome<T> rolledBack(Throwable t) {
            return new TxOutcome<>(TransactionStatusEnums.ROLLED_BACK, null, t);
        }

        public static <T> TxOutcome<T> committedWithException(Throwable t) {
            return new TxOutcome<>(TransactionStatusEnums.COMMITTED_WITH_EXCEPTION, null, t);
        }

        public TransactionStatusEnums getStatus() { return transactionStatusEnums; }
        public Optional<T> getValue() { return Optional.ofNullable(value); }
        public Optional<Throwable> getException() { return Optional.ofNullable(exception); }

        public boolean isCommitted() { return transactionStatusEnums == TransactionStatusEnums.COMMITTED; }
        public boolean isRolledBack() { return transactionStatusEnums == TransactionStatusEnums.ROLLED_BACK; }
        public boolean isCommittedWithException() { return transactionStatusEnums == TransactionStatusEnums.COMMITTED_WITH_EXCEPTION; }
    }

    /* =========================================================
     * 7) utils
     * ========================================================= */

    private static RuntimeException wrapToRuntime(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t; // Error 不建议包装吞掉
        }
        return new RuntimeException(t);
    }

    private static void safeCallback(Consumer<Throwable> c, Throwable t, String name) {
        if (c == null) return;
        try {
            c.accept(t);
        } catch (Throwable cbEx) {
            // 回调失败不影响事务主流程
            log.error("Callback {} failed", name, cbEx);
        }
    }
}
