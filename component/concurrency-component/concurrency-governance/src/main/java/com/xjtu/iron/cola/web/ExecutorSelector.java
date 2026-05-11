package com.xjtu.iron;

import com.xjtu.iron.context.GovernorContext;

import java.util.concurrent.Executor;

public interface ExecutorSelector {
    Executor select(GovernorContext context);
}
