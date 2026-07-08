package com.xjtu.iron.distributed.lock.core.spi;


import com.xjtu.iron.lock.api.LockOptions;

import java.time.Duration;

public interface LockProvider {

    String providerName();

    LockAcquireResponse acquire(LockAcquireRequest request);

    LockReleaseResponse release(LockLease lease);

    LockRenewResponse renew(LockLease lease, Duration leaseTime);

    boolean isHeld(LockLease lease);

    LockProviderCapabilities capabilities();
}
