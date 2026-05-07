package com.xjtu.iron.cola.web.template;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface GovernanceTemplate {

    <T> T execute(String resourceName, Supplier<T> supplier);

    <T> T execute(String resourceName, Supplier<T> supplier, Function<Throwable, T> fallback);

    void execute(String resourceName, Runnable runnable);

    void execute(String resourceName, Runnable runnable, Consumer<Throwable> fallback);
}
