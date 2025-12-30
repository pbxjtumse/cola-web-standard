package com.xjtu.iron.cola.web.impl.executor;

import com.xjtu.iron.cola.web.ConcurrencyGovernor;
import com.xjtu.iron.cola.web.GovernanceExecutor;
import com.xjtu.iron.cola.web.GovernorChain;
import com.xjtu.iron.cola.web.context.GovernorContext;
import com.xjtu.iron.cola.web.dto.Permit;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

@Component
public class DefaultGovernanceExecutor implements GovernanceExecutor {

    private final GovernorChain governorChain;

    public DefaultGovernanceExecutor(List<ConcurrencyGovernor> governors) {
        // Spring 注入顺序 = 治理顺序
        this.governorChain = new GovernorChain(governors);
    }

    @Override
    public <T> T execute(GovernorContext context, Callable<T> task, Executor executor) throws Exception {
        Permit permit = governorChain.tryAcquire(context);
        if (!permit.isAcquired()) {
            throw new RejectedExecutionException("Rejected by governance");
        }

        try {
            return task.call();
        } finally {
            permit.release();
        }
    }

    @Override
    public void executeAsync(GovernorContext context, Runnable task, Executor executor) {
        Permit permit = governorChain.tryAcquire(context);
        if (!permit.isAcquired()) {
            throw new RejectedExecutionException("Rejected by governance");
        }
        executor.execute(() -> {
            try {
                task.run();
            } finally {
                permit.release();
            }
        });
    }

    @Override
    public <T> CompletableFuture<T> executeFuture(GovernorContext context, Supplier<T> supplier, Executor executor) {
        Permit permit = governorChain.tryAcquire(context);
        if (!permit.isAcquired()) {
            CompletableFuture<T> rejected = new CompletableFuture<>();
            rejected.completeExceptionally(
                    new RejectedExecutionException("Rejected by governance")
            );
            return rejected;
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } finally {
                permit.release();
            }
        }, executor);
    }
}
