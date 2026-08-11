package com.xjtu.iron.idempotent.core;

@FunctionalInterface
public interface IdempotencyOwnerTokenGenerator {
    String generate(String namespace, String key);
}
