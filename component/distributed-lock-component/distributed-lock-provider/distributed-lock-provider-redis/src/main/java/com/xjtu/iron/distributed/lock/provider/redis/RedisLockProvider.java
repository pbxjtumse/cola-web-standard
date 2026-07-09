package com.xjtu.iron.distributed.lock.provider.redis;

import com.xjtu.iron.distributed.lock.core.spi.LockAcquireRequest;
import com.xjtu.iron.distributed.lock.core.spi.LockAcquireResponse;
import com.xjtu.iron.distributed.lock.core.spi.LockCheckRequest;
import com.xjtu.iron.distributed.lock.core.spi.LockCheckResponse;
import com.xjtu.iron.distributed.lock.core.spi.LockProvider;
import com.xjtu.iron.distributed.lock.core.spi.LockProviderCapabilities;
import com.xjtu.iron.distributed.lock.core.spi.LockReleaseRequest;
import com.xjtu.iron.distributed.lock.core.spi.LockReleaseResponse;
import com.xjtu.iron.distributed.lock.core.spi.LockRenewRequest;
import com.xjtu.iron.distributed.lock.core.spi.LockRenewResponse;

/**
 * Redis 分布式锁 Provider 占位实现。
 *
 * <p>当前版本先放接口和分层骨架，正式 Redis 读写实现会在下一轮接入 RedisLockScriptExecutor，并把
 * RedisLockScripts 中的 acquire/release/renew/check 脚本完整串起来。</p>
 */
public final class RedisLockProvider implements LockProvider {

    @Override
    public String providerName() {
        return RedisLockConstants.PROVIDER_NAME;
    }

    @Override
    public LockAcquireResponse acquire(LockAcquireRequest request) {
        throw new UnsupportedOperationException("RedisLockProvider acquire is not implemented yet");
    }

    @Override
    public LockReleaseResponse release(LockReleaseRequest request) {
        throw new UnsupportedOperationException("RedisLockProvider release is not implemented yet");
    }

    @Override
    public LockRenewResponse renew(LockRenewRequest request) {
        throw new UnsupportedOperationException("RedisLockProvider renew is not implemented yet");
    }

    @Override
    public LockCheckResponse check(LockCheckRequest request) {
        throw new UnsupportedOperationException("RedisLockProvider check is not implemented yet");
    }

    @Override
    public LockProviderCapabilities capabilities() {
        return LockProviderCapabilities.builder()
                .autoRenewSupported(true)
                .fencingTokenSupported(true)
                .pubSubWaitSupported(true)
                .fairLockSupported(false)
                .reentrantSupported(false)
                .build();
    }
}
