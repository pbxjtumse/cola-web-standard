package com.xjtu.iron.distributed.lock.starter.redis;

import com.xjtu.iron.distributed.lock.provider.redis.RedisLockScriptDescriptor;
import com.xjtu.iron.distributed.lock.provider.redis.RedisLockScriptExecutor;
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
 * 基于 Spring Data Redis {@link StringRedisTemplate} 的 Redis Lua 脚本执行器。
 *
 * <p>
 * 该类属于 Spring Boot starter 适配层，而不是 Redis Provider 核心实现。
 * Redis Provider 只依赖 {@link RedisLockScriptExecutor} 抽象，避免 provider 模块反向依赖 Spring Data Redis。
 * </p>
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
        RedisLockScriptDescriptor descriptor = RedisLockScriptDescriptor.fromLocation(scriptLocation);
        DefaultRedisScript<Object> script = new DefaultRedisScript<>();
        script.setScriptText(loadScriptText(descriptor.getLocation()));
        script.setResultType(resultType(descriptor));
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
    private Class<Object> resultType(RedisLockScriptDescriptor descriptor) {
        return (Class<Object>) descriptor.getResultType();
    }
}
