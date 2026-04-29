package com.xjtu.iron.cola.web;

import com.xjtu.iron.cola.web.context.GovernorContext;

import java.util.concurrent.Executor;

public interface ExecutorSelector {
    Executor select(GovernorContext context);
}
