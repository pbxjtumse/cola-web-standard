package com.xjtu.iron.transaction.api.execution;

import com.xjtu.iron.transaction.api.definition.TransactionOptions;

/**
 * 本地事务统一执行入口。
 *
 * <p>该接口只负责以统一语义执行一个本地事务单元，不负责 XA、TCC、Saga 等分布式事务协调。</p>
 * <p>一期坚持显式 API，不提供自定义 {@code @Transactional} AOP 语法糖，使事务边界在代码中可见。</p>
 */
public interface TransactionExecutor {

    /**
     * 使用默认事务选项执行有返回值业务。
     */
    default <T> T execute(TransactionCallback<T> callback) {
        // 默认场景统一走带 TransactionOptions 的主入口，避免维护两套事务执行逻辑。
        return execute(TransactionOptions.defaults(), callback);
    }

    /**
     * 使用指定事务选项执行有返回值业务。
     */
    <T> T execute(TransactionOptions options, TransactionCallback<T> callback);

    /**
     * 使用默认事务选项执行无返回值业务。
     */
    default void executeWithoutResult(TransactionRunnable runnable) {
        // 无返回值场景同样复用统一的主入口，只在最外层丢弃返回值。
        executeWithoutResult(TransactionOptions.defaults(), runnable);
    }

    /**
     * 使用指定事务选项执行无返回值业务。
     */
    default void executeWithoutResult(TransactionOptions options, TransactionRunnable runnable) {
        // 将 Runnable 适配成有返回值 callback，使事务开启、提交、回滚只存在一套实现。
        execute(options, context -> {
            runnable.execute(context);
            return null;
        });
    }
}
