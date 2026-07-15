package com.xjtu.iron.distributed.lock.provider.redis;

import com.xjtu.iron.distributed.lock.core.spi.LockProvider;
import com.xjtu.iron.distributed.lock.core.spi.LockProviderCapabilities;
import com.xjtu.iron.distributed.lock.core.spi.model.LockLease;
import com.xjtu.iron.distributed.lock.core.spi.request.LockAcquireRequest;
import com.xjtu.iron.distributed.lock.core.spi.request.LockCheckRequest;
import com.xjtu.iron.distributed.lock.core.spi.request.LockReleaseRequest;
import com.xjtu.iron.distributed.lock.core.spi.request.LockRenewRequest;
import com.xjtu.iron.distributed.lock.core.spi.response.LockAcquireResponse;
import com.xjtu.iron.distributed.lock.core.spi.response.LockCheckResponse;
import com.xjtu.iron.distributed.lock.core.spi.response.LockReleaseResponse;
import com.xjtu.iron.distributed.lock.core.spi.response.LockRenewResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Redis 分布式锁 Provider。 */
public final class RedisLockProvider implements LockProvider {

    private final RedisLockScriptExecutor scriptExecutor;
    private final RedisLockKeyBuilder keyBuilder;
    private final RedisScriptResultParser resultParser;

    public RedisLockProvider(RedisLockScriptExecutor scriptExecutor) {
        this(scriptExecutor, new RedisLockKeyBuilder(), new RedisScriptResultParser());
    }

    public RedisLockProvider(RedisLockScriptExecutor scriptExecutor, RedisLockKeyBuilder keyBuilder) {
        this(scriptExecutor, keyBuilder, new RedisScriptResultParser());
    }

    public RedisLockProvider(RedisLockScriptExecutor scriptExecutor, RedisLockKeyBuilder keyBuilder,
                             RedisScriptResultParser resultParser) {
        this.scriptExecutor = Objects.requireNonNull(scriptExecutor, "scriptExecutor must not be null");
        this.keyBuilder = Objects.requireNonNull(keyBuilder, "keyBuilder must not be null");
        this.resultParser = Objects.requireNonNull(resultParser, "resultParser must not be null");
    }

    @Override
    public String providerName() { return RedisLockConstants.PROVIDER_NAME; }

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
            RedisScriptResultParser.AcquireResult result = resultParser.parseAcquire(
                    scriptExecutor.execute(RedisLockScripts.ACQUIRE, keys, args));
            if (result.getFlag() == 1L) {
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
            Duration ttl = Duration.ofMillis(Math.max(0L, result.getRemainingTtlMillis()));
            return LockAcquireResponse.notAcquired(ttl);
        } catch (Throwable e) {
            return LockAcquireResponse.failed(e);
        }
    }

    @Override
    public LockReleaseResponse release(LockReleaseRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        try {
            long result = resultParser.parseLong(scriptExecutor.execute(RedisLockScripts.RELEASE,
                    Arrays.asList(request.getLockKey()), Arrays.asList(request.getOwnerToken())));
            if (result == 1L) { return LockReleaseResponse.released(); }
            if (result == -1L) { return LockReleaseResponse.notFound(); }
            return LockReleaseResponse.notOwner();
        } catch (Throwable e) {
            return LockReleaseResponse.failed(e);
        }
    }

    @Override
    public LockRenewResponse renew(LockRenewRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        try {
            long result = resultParser.parseLong(scriptExecutor.execute(RedisLockScripts.RENEW,
                    Arrays.asList(request.getLockKey()),
                    Arrays.asList(request.getOwnerToken(), String.valueOf(request.getLeaseTime().toMillis()))));
            if (result == 1L) { return LockRenewResponse.renewed(Instant.now().plus(request.getLeaseTime())); }
            if (result == -1L) { return LockRenewResponse.notFound(); }
            return LockRenewResponse.notOwner();
        } catch (Throwable e) {
            return LockRenewResponse.failed(e);
        }
    }

    @Override
    public LockCheckResponse check(LockCheckRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        try {
            long result = resultParser.parseLong(scriptExecutor.execute(RedisLockScripts.CHECK,
                    Arrays.asList(request.getLockKey()), Arrays.asList(request.getOwnerToken())));
            if (result == 1L) { return LockCheckResponse.held(); }
            if (result == -1L) { return LockCheckResponse.notFound(); }
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
}
