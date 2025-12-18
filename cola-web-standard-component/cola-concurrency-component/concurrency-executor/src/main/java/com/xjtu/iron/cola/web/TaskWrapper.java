package com.xjtu.iron.cola.web;

import java.util.Map;

public final class TaskWrapper {

    public static Runnable wrap(Runnable task) {
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        return () -> {
            long start = System.currentTimeMillis();
            try {
                if (mdc != null) MDC.setContextMap(mdc);
                task.run();
            } catch (Throwable t) {
                log.error("Async task failed", t);
                throw t;
            } finally {
                MDC.clear();
                log.debug("Task cost {} ms", System.currentTimeMillis() - start);
            }
        };
    }
}

