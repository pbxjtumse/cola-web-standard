package com.xjtu.iron.cola.web.metrics;

public interface MetricsService {
    void counter(String name, long value);

    void gauge(String name, double value);

    void timer(String name, long millis);
}
