-- KEYS[1] = lock key
-- KEYS[2] = fencing key
-- ARGV[1] = owner token
-- ARGV[2] = lease millis
-- ARGV[3] = fencing required: "1" or "0"

if redis.call("exists", KEYS[1]) == 0 then
    local fence = ""

    if ARGV[3] == "1" then
        fence = redis.call("incr", KEYS[2])
    end

    redis.call("psetex", KEYS[1], ARGV[2], ARGV[1])

    return {1, fence}
else
    return {0, redis.call("pttl", KEYS[1])}
end