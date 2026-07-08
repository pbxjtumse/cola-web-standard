-- KEYS[1] = lock key
-- ARGV[1] = owner token
-- ARGV[2] = lease millis

local value = redis.call("get", KEYS[1])

if not value then
    return -1
end

if value == ARGV[1] then
    redis.call("pexpire", KEYS[1], ARGV[2])
    return 1
else
    return 0
end