package com.xjtu.iron.concurrency.core.functional;

import com.xjtu.iron.concurrency.api.enums.RejectionPolicy;
import com.xjtu.iron.concurrency.api.execution.pool.ThreadPoolSnapshot;
import com.xjtu.iron.concurrency.api.execution.pool.ThreadPoolUpdateRequest;
import com.xjtu.iron.concurrency.core.execution.DefaultRejectedExecutionHandlerFactory;
import com.xjtu.iron.concurrency.core.execution.DefaultThreadPoolManager;
import com.xjtu.iron.concurrency.core.execution.DefaultThreadPoolRegistry;
import com.xjtu.iron.concurrency.core.rejection.CallerRunsRejectedExecutionHandler;
import com.xjtu.iron.concurrency.core.support.TestExecutors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DefaultThreadPoolManager 功能与边界测试")
class DefaultThreadPoolManagerFunctionalTest {

    @Test
    @DisplayName("snapshot：应返回线程池运行快照")
    void snapshot_should_return_runtime_snapshot() {
        DefaultThreadPoolRegistry registry = new DefaultThreadPoolRegistry();
        ThreadPoolExecutor executor = TestExecutors.fixed("manager", 2);
        registry.register("default", executor);
        DefaultThreadPoolManager manager = new DefaultThreadPoolManager(registry, new DefaultRejectedExecutionHandlerFactory());

        try {
            ThreadPoolSnapshot snapshot = manager.snapshot("default");

            assertThat(snapshot.getExecutorName()).isEqualTo("default");
            assertThat(snapshot.getCorePoolSize()).isEqualTo(2);
            assertThat(snapshot.getMaximumPoolSize()).isEqualTo(2);
            assertThat(snapshot.getQueueCapacity()).isGreaterThan(0);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("resize：应动态调整 corePoolSize 和 maximumPoolSize")
    void resize_should_update_core_and_max_pool_size() {
        DefaultThreadPoolRegistry registry = new DefaultThreadPoolRegistry();
        ThreadPoolExecutor executor = TestExecutors.fixed("resize", 2);
        registry.register("default", executor);
        DefaultThreadPoolManager manager = new DefaultThreadPoolManager(registry, new DefaultRejectedExecutionHandlerFactory());

        try {
            ThreadPoolSnapshot snapshot = manager.resize("default", 1, 3);

            assertThat(snapshot.getCorePoolSize()).isEqualTo(1);
            assertThat(snapshot.getMaximumPoolSize()).isEqualTo(3);
            assertThat(executor.getCorePoolSize()).isEqualTo(1);
            assertThat(executor.getMaximumPoolSize()).isEqualTo(3);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("update：应支持更新 keepAlive、核心线程超时和拒绝策略")
    void update_should_update_keep_alive_core_timeout_and_rejection_policy() {
        DefaultThreadPoolRegistry registry = new DefaultThreadPoolRegistry();
        ThreadPoolExecutor executor = TestExecutors.fixed("update", 2);
        registry.register("default", executor);
        DefaultThreadPoolManager manager = new DefaultThreadPoolManager(registry, new DefaultRejectedExecutionHandlerFactory());

        try {
            ThreadPoolUpdateRequest request = new ThreadPoolUpdateRequest();
            request.setKeepAliveTime(Duration.ofMillis(500));
            request.setAllowCoreThreadTimeout(true);
            request.setRejectionPolicy(RejectionPolicy.CALLER_RUNS);

            ThreadPoolSnapshot snapshot = manager.update("default", request);

            assertThat(executor.getKeepAliveTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isEqualTo(500L);
            assertThat(executor.allowsCoreThreadTimeOut()).isTrue();
            assertThat(executor.getRejectedExecutionHandler()).isInstanceOf(CallerRunsRejectedExecutionHandler.class);
            assertThat(snapshot.getRejectedExecutionHandler()).isEqualTo("CallerRunsRejectedExecutionHandler");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("update：core 或 max 非法时应拒绝")
    void update_should_reject_invalid_pool_size() {
        DefaultThreadPoolRegistry registry = new DefaultThreadPoolRegistry();
        ThreadPoolExecutor executor = TestExecutors.fixed("invalid", 2);
        registry.register("default", executor);
        DefaultThreadPoolManager manager = new DefaultThreadPoolManager(registry, new DefaultRejectedExecutionHandlerFactory());

        try {
            ThreadPoolUpdateRequest request = new ThreadPoolUpdateRequest();
            request.setCorePoolSize(4);
            request.setMaximumPoolSize(2);

            assertThatThrownBy(() -> manager.update("default", request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("maximumPoolSize");
        } finally {
            executor.shutdownNow();
        }
    }
}
