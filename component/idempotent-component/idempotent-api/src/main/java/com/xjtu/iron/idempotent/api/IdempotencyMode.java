package com.xjtu.iron.idempotent.api;
/** SHORT_TERM=有限窗口去重；DURABLE=长期业务幂等。 */
public enum IdempotencyMode { SHORT_TERM, DURABLE }
