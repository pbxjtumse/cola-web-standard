package com.xjtu.iron.distributed.lock.provider.redis;

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
 * 基于 Spring Data Redis StringRedisTemplate 的 Lua 脚本执行器。
 */
public class StringRedisTemplateRedisLockScriptExecutor implements RedisLockScriptExecutor {

    private final StringRedisTemplate stringRedisTemplate;
    private final Map<String, RedisScript<?>> scriptCache = new ConcurrentHashMap<>();

    public StringRedisTemplateRedisLockScriptExecutor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = Objects.requireNonNull(stringRedisTemplate, "stringRedisTemplate must not be null");
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Object execute(String scriptLocation, List<String> keys, List<String> args) {
        RedisScript script = scriptCache.computeIfAbsent(scriptLocation, this::loadScript);
        return stringRedisTemplate.execute(script, keys, args.toArray(new String[0]));
    }

    private RedisScript<?> loadScript(String scriptLocation) {
        RedisLockScriptDescriptor descriptor = RedisLockScriptDescriptor.fromLocation(scriptLocation);
        DefaultRedisScript<Object> script = new DefaultRedisScript<>();
        script.setScriptText(loadScriptText(scriptLocation));
        @SuppressWarnings("unchecked")
        Class<Object> resultType = (Class<Object>) descriptor.getResultType();
        script.setResultType(resultType);
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
}
