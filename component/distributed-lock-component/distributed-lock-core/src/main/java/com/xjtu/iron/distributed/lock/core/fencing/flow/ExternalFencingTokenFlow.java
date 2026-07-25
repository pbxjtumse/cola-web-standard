package com.xjtu.iron.distributed.lock.core.fencing.flow;

import com.xjtu.iron.distributed.lock.api.LockHandle;
import com.xjtu.iron.distributed.lock.api.LockResult;
import com.xjtu.iron.distributed.lock.core.fencing.FencingTokenMode;
import com.xjtu.iron.distributed.lock.core.fencing.FencingTokenResponse;
import com.xjtu.iron.distributed.lock.core.spi.LockProvider;
import com.xjtu.iron.distributed.lock.core.spi.model.LockLease;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** JDBC sequence 等外部发号：锁已获取，但 token 需要独立 Provider 补充。 */
public final class ExternalFencingTokenFlow implements FencingTokenFlow {

    private final FencingTokenFlowSupport support;

    public ExternalFencingTokenFlow(FencingTokenFlowSupport support) {
        this.support = Objects.requireNonNull(support, "support must not be null");
    }

    @Override
    public FencingTokenMode mode() {
        return FencingTokenMode.EXTERNAL;
    }

    @Override
    public FencingCompletion complete(FencingContext context) {
        LockProvider lockProvider = context.lockProvider();
        LockLease lease = context.lease();
        String source = context.plan().sourceName(lockProvider.providerName());

        Instant fencingStart = support.now();
        FencingTokenResponse tokenResponse = support.issueExternal(context);
        Duration fencingDuration = Duration.between(fencingStart, support.now());
        if (!tokenResponse.isIssued()) {
            return FencingCompletion.failure(support.fencingFailure(lockProvider, lease,
                    context.waitDuration(), source, tokenResponse, fencingDuration));
        }

        long token = tokenResponse.token().orElseThrow();
        LockLease fencedLease = lease.withFencingToken(token, source);
        support.recordFencingSuccess(lockProvider, fencedLease, source, fencingDuration);

        /*
         * 外部发号可能比 leaseTime 更慢。发号完成后必须重新确认 ownerToken 仍然持锁，
         * 避免把“已经过期的租约 + 后生成的较大 token”交给业务 callback。
         */
        LockResult<LockHandle> ownershipFailure = support.verifyOwnershipAfterExternalFencing(
                lockProvider, fencedLease, context.waitDuration());
        if (ownershipFailure != null) {
            return FencingCompletion.failure(ownershipFailure);
        }
        return FencingCompletion.success(fencedLease);
    }
}
