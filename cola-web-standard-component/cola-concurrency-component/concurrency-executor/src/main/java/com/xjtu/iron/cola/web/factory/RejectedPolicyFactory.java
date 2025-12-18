package com.xjtu.iron.cola.web.factory;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

public class RejectedPolicyFactory {
    public static RejectedExecutionHandler create(String policy) {
        switch (policy) {
            case "CALLER_RUNS":
                return new ThreadPoolExecutor.CallerRunsPolicy();
            case "DISCARD":
                return new ThreadPoolExecutor.DiscardPolicy();
            case "DISCARD_OLDEST":
                return new ThreadPoolExecutor.DiscardOldestPolicy();
            case "ABORT":
            default:
                return new ThreadPoolExecutor.AbortPolicy();
        }
    }
}
