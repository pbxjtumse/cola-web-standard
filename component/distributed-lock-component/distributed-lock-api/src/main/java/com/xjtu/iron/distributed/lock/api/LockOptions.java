package com.xjtu.iron.distributed.lock.api;


import java.time.Duration;

public final class LockOptions {

    private final String namespace;

    private final Duration waitTime;

    private final Duration leaseTime;

    private final LockWaitStrategy waitStrategy;

    private final boolean autoRenew;

    private final Duration renewInterval;

    private final Duration maxRenewTime;

    private final boolean fencingRequired;

    private final boolean failOnLockLost;

    private final String providerName;

    private final RetryBackoffSpec backoffSpec;

    // builder / getters
}
