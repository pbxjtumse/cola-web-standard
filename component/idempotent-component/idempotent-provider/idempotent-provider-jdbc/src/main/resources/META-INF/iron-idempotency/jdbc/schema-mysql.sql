CREATE TABLE IF NOT EXISTS iron_idempotency_record (
    id BIGINT NOT NULL AUTO_INCREMENT,

    namespace VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(256) NOT NULL,

    -- 路由元数据：未来分库分表 / Reliable Task 恢复时必须带回。
    route_key VARCHAR(256) NULL,

    -- 同一个 idempotencyKey 必须对应同一个业务请求指纹。
    request_hash VARCHAR(128) NULL,

    -- 持久状态严格保持三态：PROCESSING / SUCCESS / FAILED。
    status VARCHAR(32) NOT NULL,
    owner_token VARCHAR(128) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    result_payload TEXT NULL,
    failure_code VARCHAR(128) NULL,
    failure_message VARCHAR(1024) NULL,
    failure_retryable BOOLEAN NOT NULL DEFAULT FALSE,

    -- NONE / EXTERNAL_TASK。扫描调度不在本组件，这里只保存恢复契约。
    recovery_mode VARCHAR(32) NOT NULL DEFAULT 'NONE',

    -- SHORT_TERM 窗口策略；DURABLE 下仅作为元数据存在。
    window_policy VARCHAR(64) NOT NULL DEFAULT 'FIXED_FROM_FIRST_ACQUIRE',

    processing_expire_at TIMESTAMP(3) NULL,
    window_expire_at TIMESTAMP(3) NULL,
    retention_expire_at TIMESTAMP(3) NULL,

    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    completed_at TIMESTAMP(3) NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_iron_idempotency_namespace_key (namespace, idempotency_key),
    KEY idx_iron_idempotency_recovery (
        recovery_mode, status, processing_expire_at, updated_at
    ),
    KEY idx_iron_idempotency_route (route_key, status, processing_expire_at)
);
