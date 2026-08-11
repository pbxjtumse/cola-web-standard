package com.xjtu.iron.idempotent.api;

/**
 * 一次 IdempotencyExecutor 调用的最终状态。
 */
public enum IdempotencyResultStatus {EXECUTED, REPLAYED, PROCESSING, PREVIOUS_FAILED, KEY_CONFLICT, LOCK_NOT_ACQUIRED, EXECUTION_FAILED, OWNERSHIP_LOST, RESULT_CODEC_ERROR, REPOSITORY_ERROR, INVALID_OPTIONS}
