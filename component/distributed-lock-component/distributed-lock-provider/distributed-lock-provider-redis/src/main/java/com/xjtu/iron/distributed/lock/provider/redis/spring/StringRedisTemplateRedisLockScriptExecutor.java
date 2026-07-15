package com.xjtu.iron.distributed.lock.provider.redis.spring;

import com.xjtu.iron.distributed.lock.provider.redis.RedisLockScriptExecutor;
import com.xjtu.iron.distributed.lock.provider.redis.RedisLockScripts;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 {@link StringRedisTemplate} 的 Redis Lua 脚本执行器。
 *
 * <p>
 * 本类只负责把 classpath 下的 Lua 脚本加载成 Spring Data Redis 的 {@link RedisScript} 并执行。
 * Redis 分布式锁的业务语义仍然由 {@code RedisLockProvider} 负责解析。
 * </p>
 *
 * <p>
 * 返回类型约定：
 * </p>
 *
 * <ul>
 *     <li>{@code acquire.lua} 返回 {@code List}，形如 {@code [1, fence]} 或 {@code [0, ttl]}；</li>
 *     <li>{@code release.lua}、{@code renew.lua}、{@code check.lua} 返回 {@code Long}。</li>
 * </ul>
 */
public final class StringRedisTemplateRedisLockScriptExecutor implements RedisLockScriptExecutor {

    private final StringRedisTemplate stringRedisTemplate;

    private final Map<String, RedisScript<?>> scriptCache = new ConcurrentHashMap<>();

    public StringRedisTemplateRedisLockScriptExecutor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = Objects.requireNonNull(stringRedisTemplate, "stringRedisTemplate must not be null");
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Object execute(String scriptLocation, List<String> keys, List<String> args) {
        Objects.requireNonNull(scriptLocation, "scriptLocation must not be null");
        Objects.requireNonNull(keys, "keys must not be null");
        Objects.requireNonNull(args, "args must not be null");

        RedisScript script = scriptCache.computeIfAbsent(scriptLocation, this::loadScript);
        return stringRedisTemplate.execute(script, keys, args.toArray(new String[0]));
    }

    private RedisScript<?> loadScript(String scriptLocation) {
        DefaultRedisScript<Object> script = new DefaultRedisScript<>();
        script.setScriptText(loadScriptText(scriptLocation));
        script.setResultType(resultType(scriptLocation));
        return script;
    }

    private String loadScriptText(String scriptLocation) {
        ClassPathResource resource = new ClassPathResource(scriptLocation);
        if (!resource.exists()) {
            throw new IllegalArgumentException("redis lua script not found: " + scriptLocation);
        }
        try {
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("failed to load redis lua script: " + scriptLocation, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Class<Object> resultType(String scriptLocation) {
        if (RedisLockScripts.ACQUIRE.equals(scriptLocation)) {
            return (Class<Object>) (Class<?>) List.class;
        }
        if (RedisLockScripts.RELEASE.equals(scriptLocation)
                || RedisLockScripts.RENEW.equals(scriptLocation)
                || RedisLockScripts.CHECK.equals(scriptLocation)) {
            return (Class<Object>) (Class<?>) Long.class;
        }
        return Object.class;
    }
}
