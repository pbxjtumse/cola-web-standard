package com.xjtu.iron.idempotent.core;

import java.util.UUID;

public final class UuidIdempotencyOwnerTokenGenerator implements IdempotencyOwnerTokenGenerator {
    public String generate(String namespace, String key) {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
