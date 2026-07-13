package com.xjtu.iron.distributed.lock.provider.redis;

import com.xjtu.iron.distributed.lock.core.spi.LockAcquireRequest;
import com.xjtu.iron.distributed.lock.core.spi.LockAcquireResponse;
import com.xjtu.iron.distributed.lock.core.spi.LockCheckRequest;
import com.xjtu.iron.distributed.lock.core.spi.LockCheckResponse;
import com.xjtu.iron.distributed.lock.core.spi.LockLease;
import com.xjtu.iron.distributed.lock.core.spi.LockProvider;
import com.xjtu.iron.distributed.lock.core.spi.LockProviderCapabilities;
import com.xjtu.iron.distributed.lock.core.spi.LockReleaseRequest;
import com.xjtu.iron.distributed.lock.core.spi.LockReleaseResponse;
import com.xjtu.iron.distributed.lock.core.spi.LockRenewRequest;
import com.xjtu.iron.distributed.lock.core.spi.LockRenewResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Redis 分布式锁 Provider。
 *
 * <p>该 Provider 通过 Lua 脚本实现 acquire / release / renew / check 的原子操作。Redis 客户端细节由
 * {@link RedisLockScriptExecutor} 屏蔽，因此本类不直接依赖 Jedis、Lettuce 或 Spring Data Redis。</p>
 *
 * <p>一期能力边界：</p>
 * <ul>
 *     <li>支持 ownerToken。</li>
 *     <li>支持 Lua 安全释放。</li>
 *     <li>支持 Lua 安全续期。</li>
 *     <li>不支持 fencing token。</li>
 *     <li>不支持 Pub/Sub 等待和公平锁。</li>
 * </ul>
 */
public final class RedisLockProvider implements LockProvider {

    /** Lua 脚本执行器。 */
    private final RedisLockScriptExecutor scriptExecutor;

    /** Redis 物理 key 构造器。 */
    private final RedisLockKeyBuilder keyBuilder;

    public RedisLockProvider(RedisLockScriptExecutor scriptExecutor) {
        this(scriptExecutor, new RedisLockKeyBuilder());
    }

    public RedisLockProvider(RedisLockScriptExecutor scriptExecutor, RedisLockKeyBuilder keyBuilder) {
        this.scriptExecutor = Objects.requireNonNull(scriptExecutor, "scriptExecutor must not be null");
        this.keyBuilder = Objects.requireNonNull(keyBuilder, "keyBuilder must not be null");
    }

    @Override
    public String providerName() {
        return RedisLockConstants.PROVIDER_NAME;
    }

    @Override
    public LockAcquireResponse acquire(LockAcquireRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        if (request.getOptions().isFencingRequired()) {
            return LockAcquireResponse.failed(new UnsupportedOperationException("Redis fencing token is not supported in phase one"));
        }
        String lockKey = keyBuilder.buildLockKey(request.getNamespace(), request.getLockName());
        String fencingKey = keyBuilder.buildFencingKey(request.getNamespace(), request.getLockName());
        List<String> keys = Arrays.asList(lockKey, fencingKey);
        List<String> args = Arrays.asList(
                request.getOwnerToken(),
                String.valueOf(request.getOptions().getLeaseTime().toMillis()),
                "0"
        );
        try {
            Object raw = scriptExecutor.execute(RedisLockScripts.ACQUIRE, keys, args);
            List<?> values = asList(raw);
            long flag = asLong(values.get(0));
            if (flag == 1L) {
                Instant acquiredAt = Instant.now();
                LockLease lease = LockLease.builder()
                        .providerName(providerName())
                        .namespace(request.getNamespace())
                        .lockName(request.getLockName())
                        .lockKey(lockKey)
                        .ownerToken(request.getOwnerToken())
                        .leaseTime(request.getOptions().getLeaseTime())
                        .acquiredAt(acquiredAt)
                        .expireAt(acquiredAt.plus(request.getOptions().getLeaseTime()))
                        .build();
                return LockAcquireResponse.acquired(lease);
            }
            Duration ttl = values.size() > 1 ? Duration.ofMillis(Math.max(0L, asLong(values.get(1)))) : null;
            return LockAcquireResponse.notAcquired(ttl);
        } catch (Throwable e) {
            return LockAcquireResponse.failed(e);
        }
    }

    @Override
    public LockReleaseResponse release(LockReleaseRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        try {
            Object raw = scriptExecutor.execute(RedisLockScripts.RELEASE,
                    Arrays.asList(request.getLockKey()),
                    Arrays.asList(request.getOwnerToken()));
            long result = asLong(raw);
            if (result == 1L) {
                return LockReleaseResponse.released();
            }
            if (result == -1L) {
                return LockReleaseResponse.notFound();
            }
            return LockReleaseResponse.notOwner();
        } catch (Throwable e) {
            return LockReleaseResponse.failed(e);
        }
    }

    @Override
    public LockRenewResponse renew(LockRenewRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        try {
            Object raw = scriptExecutor.execute(RedisLockScripts.RENEW,
                    Arrays.asList(request.getLockKey()),
                    Arrays.asList(request.getOwnerToken(), String.valueOf(request.getLeaseTime().toMillis())));
            long result = asLong(raw);
            if (result == 1L) {
                return LockRenewResponse.renewed(Instant.now().plus(request.getLeaseTime()));
            }
            if (result == -1L) {
                return LockRenewResponse.notFound();
            }
            return LockRenewResponse.notOwner();
        } catch (Throwable e) {
            return LockRenewResponse.failed(e);
        }
    }

    @Override
    public LockCheckResponse check(LockCheckRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        try {
            Object raw = scriptExecutor.execute(RedisLockScripts.CHECK,
                    Arrays.asList(request.getLockKey()),
                    Arrays.asList(request.getOwnerToken()));
            long result = asLong(raw);
            if (result == 1L) {
                return LockCheckResponse.held();
            }
            if (result == -1L) {
                return LockCheckResponse.notFound();
            }
            return LockCheckResponse.notOwner();
        } catch (Throwable e) {
            return LockCheckResponse.failed(e);
        }
    }

    @Override
    public LockProviderCapabilities capabilities() {
        return LockProviderCapabilities.builder()
                .autoRenewSupported(true)
                .fencingTokenSupported(false)
                .pubSubWaitSupported(false)
                .fairLockSupported(false)
                .reentrantSupported(false)
                .build();
    }

    private static List<?> asList(Object raw) {
        if (raw instanceof List) {
            return (List<?>) raw;
        }
        if (raw instanceof Object[]) {
            return Arrays.asList((Object[]) raw);
        }
        throw new IllegalArgumentException("redis script result is not a list: " + raw);
    }

    private static long asLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof byte[]) {
            return Long.parseLong(new String((byte[]) value));
        }
        return Long.parseLong(String.valueOf(value));
    }
}
