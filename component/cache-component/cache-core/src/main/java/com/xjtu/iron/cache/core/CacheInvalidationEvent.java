package com.xjtu.iron.cache.core;

import com.xjtu.iron.cache.api.CacheKey;

/**
 * 缓存失效事件。
 *
 * <p>这是一个 provider 无关的事件模型。</p>
 *
 * <p>它不叫 RedisCacheInvalidationMessage，是为了避免把 Redis 的概念扩散到 core 层。
 * Redis、MQ、HTTP、配置中心等不同实现都可以复用这个事件模型。</p>
 *
 * <p>二期第一步中，这个事件会被 Redis Pub/Sub 发布和订阅。</p>
 */
public class CacheInvalidationEvent {

    /**
     * 缓存名称。
     *
     * <p>例如 campaignRule、merchantConfig、userProfile。</p>
     */
    private String cacheName;

    /**
     * 缓存命名空间。
     *
     * <p>通常用于区分业务域或系统，例如 marketing、order、merchant。</p>
     */
    private String namespace;

    /**
     * 业务 key。
     *
     * <p>例如 campaignId:10001。</p>
     */
    private String bizKey;

    /**
     * key 版本。
     *
     * <p>用于缓存结构升级时隔离旧 key。</p>
     */
    private String version;

    /**
     * 发布该事件的应用实例 ID。
     *
     * <p>订阅端可以用它判断消息是否由自己发布。
     * 如果是自己发布的事件，可以跳过处理，因为本实例在 evict 时已经删除过本地缓存。</p>
     */
    private String sourceInstanceId;

    /**
     * 事件产生时间。
     *
     * <p>主要用于日志、排查和后续指标统计。</p>
     */
    private long timestamp;

    /**
     * Jackson 反序列化需要无参构造方法。
     */
    public CacheInvalidationEvent() {
    }

    /**
     * 创建缓存失效事件。
     *
     * @param cacheName 缓存名称
     * @param namespace 命名空间
     * @param bizKey 业务 key
     * @param version key 版本
     * @param sourceInstanceId 事件来源实例 ID
     * @param timestamp 事件产生时间
     */
    public CacheInvalidationEvent(
            String cacheName,
            String namespace,
            String bizKey,
            String version,
            String sourceInstanceId,
            long timestamp
    ) {
        this.cacheName = cacheName;
        this.namespace = namespace;
        this.bizKey = bizKey;
        this.version = version;
        this.sourceInstanceId = sourceInstanceId;
        this.timestamp = timestamp;
    }

    /**
     * 根据 CacheKey 创建缓存失效事件。
     *
     * @param key 缓存 key
     * @param sourceInstanceId 当前应用实例 ID
     * @return 缓存失效事件
     */
    public static CacheInvalidationEvent from(CacheKey key, String sourceInstanceId) {
        return new CacheInvalidationEvent(
                key.cacheName(),
                key.namespace(),
                key.bizKey(),
                key.version(),
                sourceInstanceId,
                System.currentTimeMillis()
        );
    }

    /**
     * 将事件还原成 CacheKey。
     *
     * <p>订阅端收到事件后，需要用 CacheKey 删除本地缓存。</p>
     *
     * @return 缓存 key
     */
    public CacheKey toCacheKey() {
        return CacheKey.of(cacheName, namespace, bizKey, version);
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getBizKey() {
        return bizKey;
    }

    public void setBizKey(String bizKey) {
        this.bizKey = bizKey;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSourceInstanceId() {
        return sourceInstanceId;
    }

    public void setSourceInstanceId(String sourceInstanceId) {
        this.sourceInstanceId = sourceInstanceId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}