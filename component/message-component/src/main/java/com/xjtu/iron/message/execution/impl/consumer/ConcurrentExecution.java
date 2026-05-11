package com.xjtu.iron.message.execution.impl.consumer;

import com.xjtu.iron.message.execution.ConsumeExecution;

import java.util.concurrent.Executor;

public class ConcurrentExecution implements ConsumeExecution {

    private final Executor executor;

    public ConcurrentExecution(Executor executor) {
        this.executor = executor;
    }


}

