-- ============================================================================
-- mark-failed.lua
--
-- 作用：把当前 generation 从 PROCESSING 原子转换为 FAILED。
--
-- 注意：failureRetryable=true 只代表“未来允许显式 recover()”，
-- 并不表示普通 execute() 会在这里自动重试。
--
-- ARGV:
-- 1 ownerToken
-- 2 version
-- 3 failureCode
-- 4 failureMessage
-- 5 retryable (1/0)
-- 6 nowMs
-- 7 mode
-- 8 idempotencyWindowMs
-- 9 windowPolicy
-- 10 recordRetentionTtlMs
--
-- 返回码：
-- 1 UPDATED
-- 2 NOT_FOUND
-- 3 STALE_OWNER
-- 4 ALREADY_FINAL
-- ============================================================================

local key = KEYS[1]

local function h(name)
    local value = redis.call('HGET', key, name)
    if not value then
        return ''
    end
    return value
end

local function snapshot(code)
    return {
        tostring(code),
        h('namespace'),
        h('key'),
        h('route_key'),
        h('request_hash'),
        h('status'),
        h('owner_token'),
        h('version'),
        h('result_payload'),
        h('failure_code'),
        h('failure_message'),
        h('failure_retryable'),
        h('recovery_mode'),
        h('window_policy'),
        h('processing_expire_at'),
        h('window_expire_at'),
        h('retention_expire_at'),
        h('created_at'),
        h('updated_at'),
        h('completed_at')
    }
end

if redis.call('EXISTS', key) == 0 then
    return {'2'}
end

if h('status') ~= 'PROCESSING' then
    return snapshot(4)
end

-- 与 mark-success 一样，FAILED 也只能由当前 generation 写入。
if h('owner_token') ~= ARGV[1] or h('version') ~= ARGV[2] then
    return snapshot(3)
end

local now = tonumber(ARGV[6])
local mode = ARGV[7]
local windowMs = tonumber(ARGV[8])
local policy = ARGV[9]
local retentionMs = tonumber(ARGV[10])

if mode == 'SHORT_TERM' and policy == 'SLIDING_ON_ACCESS' then
    local windowExpireAt = now + windowMs
    local retentionExpireAt = windowExpireAt + retentionMs

    redis.call('HSET', key,
        'window_expire_at', tostring(windowExpireAt),
        'retention_expire_at', tostring(retentionExpireAt))
    redis.call('PEXPIREAT', key, retentionExpireAt)
end

redis.call('HSET', key,
    'status', 'FAILED',
    'failure_code', ARGV[3],
    'failure_message', ARGV[4],
    'failure_retryable', ARGV[5],
    'processing_expire_at', '',
    'updated_at', ARGV[6])

return snapshot(1)
