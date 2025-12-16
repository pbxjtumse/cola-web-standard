package com.xjtu.iron.cola.web.execution.impl.consumer;

import com.xjtu.iron.cola.web.execution.ConsumeExecution;
import com.xjtu.iron.cola.web.Message;

import java.util.concurrent.Executor;

public class ConcurrentExecution implements ConsumeExecution {

    private final Executor executor;

    public ConcurrentExecution(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void execute(Message<?> message, Runnable handler) {executor.execute(handler);
    }
}

