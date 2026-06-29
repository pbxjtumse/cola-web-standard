package com.xjtu.iron.concurrency.core.boundary;

import com.xjtu.iron.concurrency.api.exception.ThreadPoolNotFoundException;
import com.xjtu.iron.concurrency.core.execution.DefaultThreadPoolRegistry;
import com.xjtu.iron.concurrency.core.support.TestExecutors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DefaultThreadPoolRegistry 边界测试")
class DefaultThreadPoolRegistryBoundaryTest {

    @Test
    @DisplayName("register / getExecutor：注册后应能按名称获取同一线程池")
    void register_should_allow_get_executor_by_name() {
        DefaultThreadPoolRegistry registry = new DefaultThreadPoolRegistry();
        ThreadPoolExecutor executor = TestExecutors.fixed("registry", 1);

        try {
            registry.register("default", executor);

            assertThat(registry.getExecutor("default")).isSameAs(executor);
            assertThat(registry.snapshot()).containsKey("default");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("register：同名不同线程池重复注册时应拒绝")
    void register_should_reject_duplicate_name_with_different_executor() {
        DefaultThreadPoolRegistry registry = new DefaultThreadPoolRegistry();
        ThreadPoolExecutor first = TestExecutors.fixed("first", 1);
        ThreadPoolExecutor second = TestExecutors.fixed("second", 1);

        try {
            registry.register("default", first);

            assertThatThrownBy(() -> registry.register("default", second))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("default");
        } finally {
            first.shutdownNow();
            second.shutdownNow();
        }
    }

    @Test
    @DisplayName("getExecutor：线程池不存在时应抛出 ThreadPoolNotFoundException")
    void getExecutor_should_throw_when_not_found() {
        DefaultThreadPoolRegistry registry = new DefaultThreadPoolRegistry();

        assertThatThrownBy(() -> registry.getExecutor("missing"))
                .isInstanceOf(ThreadPoolNotFoundException.class)
                .hasMessageContaining("missing");
    }
}
