-- KEYS[1] = lock key
-- ARGV[1] = owner token

local value = redis.call("get", KEYS[1])

if value == ARGV[1] then
    return 1
else
    return 0
end