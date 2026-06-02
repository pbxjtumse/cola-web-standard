package com.xjtu.iron.cache.core.event;


import com.xjtu.iron.cache.api.key.CacheKey;

import java.time.Instant;
import java.util.UUID;

/**
 * 缓存事件。
 *
 * <p>用于描述缓存组件内部发生的关键事件。</p>
 *
 * <p>二期第一版主要用于：</p>
 *
 * <pre>
 * Redis Pub/Sub 广播本地缓存失效事件。
 * </pre>
 *
 * <p>注意：</p>
 *
 * <pre>
 * 这个事件不是业务事件；
 * 它只用于缓存组件内部治理。
 * </pre>
 */
public class CacheEvent {

    /**
     * 事件唯一 ID。
     *
     * <p>用于日志追踪和排查重复事件。</p>
     */
    private String eventId;

    /**
     * 事件类型。
     */
    private CacheEventType eventType;

    /**
     * 缓存名称。
     *
     * <p>例如 campaignRule、merchantConfig。</p>
     */
    private String cacheName;

    /**
     * 命名空间。
     *
     * <p>例如 marketing、merchant。</p>
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
     * <p>例如 v1。</p>
     */
    private String version;

    /**
     * 完整逻辑 key。
     *
     * <p>不包含 Redis keyPrefix。</p>
     */
    private String fullKey;

    /**
     * 事件来源应用。
     *
     * <p>例如 cache-demo。</p>
     */
    private String sourceApp;

    /**
     * 事件来源实例 ID。
     *
     * <p>用于消费者判断是否是自己发布的事件。</p>
     */
    private String sourceInstanceId;

    /**
     * 事件产生时间。
     */
    private long timestamp;

    /**
     * 事件原因。
     *
     * <p>例如 manual_evict、data_updated。</p>
     */
    private String reason;

    public static CacheEvent evictKey(
            CacheKey key,
            String sourceApp,
            String sourceInstanceId,
            String reason
    ) {
        CacheEvent event = new CacheEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(CacheEventType.EVICT_KEY);
        event.setCacheName(key.cacheName());
        event.setNamespace(key.namespace());
        event.setBizKey(key.bizKey());
        event.setVersion(key.version());
        event.setFullKey(key.fullKey());
        event.setSourceApp(sourceApp);
        event.setSourceInstanceId(sourceInstanceId);
        event.setTimestamp(Instant.now().toEpochMilli());
        event.setReason(reason);
        return event;
    }

    public CacheKey toCacheKey() {
        return CacheKey.of(cacheName, namespace, bizKey, version);
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public CacheEventType getEventType() {
        return eventType;
    }

    public void setEventType(CacheEventType eventType) {
        this.eventType = eventType;
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

    public String getFullKey() {
        return fullKey;
    }

    public void setFullKey(String fullKey) {
        this.fullKey = fullKey;
    }

    public String getSourceApp() {
        return sourceApp;
    }

    public void setSourceApp(String sourceApp) {
        this.sourceApp = sourceApp;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
