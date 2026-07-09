package com.xjtu.iron.distributed.lock.core.spi;

/**
 * LockProvider 注册表。
 *
 * <p>业务可以通过 LockOptions 指定 providerName；如果未指定，则使用默认 Provider。Starter 自动装配时会把
 * Redis、Redisson、ZK 等 Provider 注册到本对象中。</p>
 */
public interface LockProviderRegistry {

    /**
     * 获取默认 Provider。
     *
     * @return 默认 Provider。
     */
    LockProvider getDefaultProvider();

    /**
     * 根据名称获取 Provider。
     *
     * @param providerName Provider 名称。
     * @return Provider。
     */
    LockProvider getProvider(String providerName);

    /**
     * 判断指定 Provider 是否存在。
     *
     * @param providerName Provider 名称。
     * @return 存在返回 true。
     */
    boolean containsProvider(String providerName);
}
