package com.xjtu.iron.idempotent.api;

@FunctionalInterface
public interface IdempotencyCallback<T> {
    T doWithIdempotency(IdempotencyContext context) throws Exception;
}
