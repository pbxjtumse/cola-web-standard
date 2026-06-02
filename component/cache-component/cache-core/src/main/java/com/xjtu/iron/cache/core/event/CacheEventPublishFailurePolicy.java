package com.xjtu.iron.cache.core.event;


/**
 * 缓存事件发布失败策略。
 *
 * <p>用于控制 cacheClient.evict(key) 删除缓存成功后，
 * 如果发布 Redis Pub/Sub 事件失败，是否影响主流程。</p>
 */
public enum CacheEventPublishFailurePolicy {

    /**
     * 忽略事件发布失败。
     *
     * <p>默认推荐策略。</p>
     *
     * <p>原因：</p>
     *
     * <pre>
     * 1. 删除当前实例 L1 和 Redis L2 是主流程；
     * 2. 发布事件只是为了通知其他实例删除 L1；
     * 3. 如果事件发布失败就导致业务 evict 失败，可能扩大故障影响。
     * </pre>
     */
    IGNORE,

    /**
     * 事件发布失败时抛出异常。
     *
     * <p>适合对本地缓存一致性要求更高的场景。</p>
     */
    THROW
}
