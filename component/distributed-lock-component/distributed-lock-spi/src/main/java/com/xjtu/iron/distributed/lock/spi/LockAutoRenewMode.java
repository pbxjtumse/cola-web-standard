package com.xjtu.iron.distributed.lock.spi;

/**
 * 自动续期由谁负责。
 *
 * <p>这个枚举解决一个很重要的 Provider 差异：</p>
 * <ul>
 *     <li>自研 Redis Lua Provider：由组件自己的 {@code ScheduledLockWatchdog} 周期调用 renew.lua；</li>
 *     <li>Redisson Provider：由 Redisson 内部 watchdog 延长 TTL，组件 watchdog 只负责监视失锁和 maxRenewTime；</li>
 *     <li>未来某些 Provider 可能完全不支持自动续期。</li>
 * </ul>
 *
 * <p>因此不能再用一个 boolean 把“支持自动续期”和“谁负责续期”混在一起。</p>
 */
public enum LockAutoRenewMode {

    /** Provider 不支持自动续期。 */
    UNSUPPORTED,

    /** 由 iron-lock Core watchdog 主动调用 {@code LockProvider.renew()}。 */
    CORE_MANAGED,

    /** 由 Provider 自己维护续期，例如 Redisson watchdog。 */
    PROVIDER_MANAGED
}
