-- KEYS[1] = lock key
-- ARGV[1] = owner token
-- 1   释放成功 0   ownerToken 不匹配  -1  key 不存在

local value = redis.call("get", KEYS[1])

if not value then
    return -1
end

if value == ARGV[1] then
    return redis.call("del", KEYS[1])
else
    return 0
end