package com.xjtu.iron.transaction.api;

/**
 * 本地事务统一执行入口。
 *
 * <p>该接口只描述“如何以统一语义执行一个本地事务单元”，不负责分布式事务协调。
 * 底层事务的 begin/commit/rollback 由 TransactionProvider 适配具体事务基础设施完成。</p>
 *
 * <p>一期坚持显式 API，不提供自定义 {@code @Transactional} 注解和 AOP 代理，
 * 目的是让事务边界在代码中清晰可见，也避免自调用等代理语义影响组件行为。</p>
 */
public interface TransactionExecutor {

    /**
     * 使用默认事务选项执行有返回值事务。
     */
    default <T> T execute(TransactionCallback<T> callback) {
        return execute(TransactionOptions.defaults(), callback);
    }

    /**
     * 使用指定事务选项执行有返回值事务。
     */
    <T> T execute(TransactionOptions options, TransactionCallback<T> callback);

    /**
     * 使用默认事务选项执行无返回值事务。
     */
    default void executeWithoutResult(TransactionRunnable runnable) {
        executeWithoutResult(TransactionOptions.defaults(), runnable);
    }

    /**
     * 使用指定事务选项执行无返回值事务。
     */
    default void executeWithoutResult(TransactionOptions options, TransactionRunnable runnable) {
        execute(options, context -> {
            runnable.execute(context);
            return null;
        });
    }
}
