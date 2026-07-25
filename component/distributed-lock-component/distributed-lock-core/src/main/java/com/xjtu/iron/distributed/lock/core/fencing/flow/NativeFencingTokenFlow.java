package com.xjtu.iron.distributed.lock.core.fencing.flow;

import com.xjtu.iron.distributed.lock.api.exception.LockProviderException;
import com.xjtu.iron.distributed.lock.core.fencing.FencingTokenMode;
import com.xjtu.iron.distributed.lock.core.fencing.FencingTokenResponse;
import com.xjtu.iron.distributed.lock.core.spi.LockProvider;
import com.xjtu.iron.distributed.lock.core.spi.model.LockLease;

import java.time.Duration;
import java.util.Objects;

/** Redis/Etcd 等 LockProvider 原生发号：token 应该已经包含在 LockLease 中。 */
public final class NativeFencingTokenFlow implements FencingTokenFlow {

    private final FencingTokenFlowSupport support;

    public NativeFencingTokenFlow(FencingTokenFlowSupport support) {
        this.support = Objects.requireNonNull(support, "support must not be null");
    }

    @Override
    public FencingTokenMode mode() {
        return FencingTokenMode.NATIVE;
    }

    @Override
    public FencingCompletion complete(FencingContext context) {
        LockProvider lockProvider = context.lockProvider();
        LockLease lease = context.lease();
        if (lease.fencingToken().isEmpty()) {
            FencingTokenResponse response = FencingTokenResponse.failed(
                    new LockProviderException("native fencing token is missing: " + lockProvider.providerName()));
            return FencingCompletion.failure(support.fencingFailure(lockProvider, lease,
                    context.waitDuration(), lockProvider.providerName(), response, Duration.ZERO));
        }
        support.recordFencingSuccess(lockProvider, lease, lockProvider.providerName(), Duration.ZERO);
        return FencingCompletion.success(lease);
    }
}
