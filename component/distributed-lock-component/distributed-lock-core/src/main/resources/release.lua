-- KEYS[1] = lock key
-- KEYS[2] = release channel
-- ARGV[1] = owner token
-- ARGV[2] = release message

local value = redis.call("get", KEYS[1])

if not value then
    return -1
end

if value == ARGV[1] then
    redis.call("del", KEYS[1])
    redis.call("publish", KEYS[2], ARGV[2])
    return 1
else
    return 0
end