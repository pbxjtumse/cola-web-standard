package com.xjtu.iron.idempotent.core.observation;

import com.xjtu.iron.idempotent.api.*;

import java.time.Instant;

/**
 * 单一事件模型，避免事件类爆炸。
 */
public final class IdempotencyEvent {
    private final IdempotencyEventType type;
    private final IdempotencyStage stage;
    private final IdempotencyMode mode;
    private final String repository;
    private final Instant occurredAt;
    private final Throwable error;

    public IdempotencyEvent(IdempotencyEventType type, IdempotencyStage stage, IdempotencyMode mode, String repository, Instant occurredAt, Throwable error) {
        this.type = type;
        this.stage = stage;
        this.mode = mode;
        this.repository = repository;
        this.occurredAt = occurredAt;
        this.error = error;
    }

    public IdempotencyEventType getType() {
        return type;
    }

    public IdempotencyStage getStage() {
        return stage;
    }

    public IdempotencyMode getMode() {
        return mode;
    }

    public String getRepository() {
        return repository;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Throwable getError() {
        return error;
    }
}
