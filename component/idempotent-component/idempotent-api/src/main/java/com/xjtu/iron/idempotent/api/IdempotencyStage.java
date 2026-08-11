package com.xjtu.iron.idempotent.api;
public enum IdempotencyStage { VALIDATE, LOCK, ACQUIRE_STATE, EXECUTE, COMPLETE_STATE, REPLAY }
