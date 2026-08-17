-- ============================================================================
-- mark-success.lua
--
-- 作用：把当前 generation 从 PROCESSING 原子转换为 SUCCESS。
--
-- 正确性核心：
-- 1. 记录必须存在；
-- 2. 当前持久状态必须仍然是 PROCESSING；
-- 3. owner_token 和 version 必须与调用者完全一致；
-- 4. 只有满足以上条件，旧执行者才有资格写 SUCCESS。
--
-- 这正是为了处理：
-- A(version=1) 超时 -> B(version=2) 接管 -> A 恢复。
-- 此时 A 的 owner/version 已经过时，必须返回 STALE_OWNER，而不能覆盖 B。
--
-- ARGV:
-- 1 ownerToken
-- 2 version
-- 3 resultPayload
-- 4 nowMs
-- 5 mode
-- 6 idempotencyWindowMs
-- 7 windowPolicy
-- 8 recordRetentionTtlMs
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

-- 返回当前记录快照，Java 侧按固定字段顺序解析。
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

-- 已经不是 PROCESSING，说明记录已经完成或被其他流程改变。
if h('status') ~= 'PROCESSING' then
    return snapshot(4)
end

-- owner + version 双重校验：任何一项不匹配都视为旧 generation。
if h('owner_token') ~= ARGV[1] or h('version') ~= ARGV[2] then
    return snapshot(3)
end

local now = tonumber(ARGV[4])
local mode = ARGV[5]
local windowMs = tonumber(ARGV[6])
local policy = ARGV[7]
local retentionMs = tonumber(ARGV[8])

-- SLIDING_ON_ACCESS 明确要求“本次有效访问后重新计算窗口”。
-- FIXED_FROM_FIRST_ACQUIRE 不进入这里，因此首次 windowExpireAt 保持不变。
if (mode == 'WINDOWED' or mode == 'SHORT_TERM') and policy == 'SLIDING_ON_ACCESS' then
    local windowExpireAt = now + windowMs
    local retentionExpireAt = windowExpireAt + retentionMs

    redis.call('HSET', key,
        'window_expire_at', tostring(windowExpireAt),
        'retention_expire_at', tostring(retentionExpireAt))

    -- 物理 Redis Key 可以比语义窗口多保留 retention 时间，
    -- 但新的 tryAcquire 会根据 window_expire_at 判断是否允许开启新 generation。
    redis.call('PEXPIREAT', key, retentionExpireAt)
end

redis.call('HSET', key,
    'status', 'SUCCESS',
    'result_payload', ARGV[3],
    'failure_code', '',
    'failure_message', '',
    'failure_retryable', '0',
    'processing_expire_at', '',
    'completed_at', ARGV[4],
    'updated_at', ARGV[4])

return snapshot(1)
