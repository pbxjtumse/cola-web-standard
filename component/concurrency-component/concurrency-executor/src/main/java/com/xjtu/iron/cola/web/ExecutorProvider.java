package com.xjtu.iron.cola.web;

import com.xjtu.iron.cola.web.registry.ThreadPoolRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 *
 * @author pbxjt
 * @date 2025/12/26
 */
@Component
public class ExecutorProvider {
    /**
     *
     */
    private final ThreadPoolRegistry registry;

    /**
     * @param registry
     */
    public ExecutorProvider(ThreadPoolRegistry registry) {
        this.registry = registry;
    }

    /**
     * @param name
     * @return {@link Executor }
     */
    public Executor get(String name) {
        return registry.get(name);
    }
}
