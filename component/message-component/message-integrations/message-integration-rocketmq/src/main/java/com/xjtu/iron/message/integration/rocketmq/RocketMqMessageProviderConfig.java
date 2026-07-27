package com.xjtu.iron.message.integration.rocketmq;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Objects;

/**
 * 表示 RocketMQ 5.x gRPC Java Client 的一期基础配置。
 *
 * <p>{@code endpoints}：gRPC Proxy 接入地址</p>
 * <p>{@code topics}：Producer 启动时预声明的物理 Topic</p>
 * <p>{@code requestTimeout}：客户端请求超时</p>
 * <p>{@code maxAttempts}：RocketMQ 客户端内部最大尝试次数</p>
 * <p>{@code accessKey}：可选访问密钥</p>
 * <p>{@code secretKey}：可选访问密钥</p>
 */
public final class RocketMqMessageProviderConfig {
    /** gRPC Proxy 接入地址。 */
    private final String endpoints;

    /** Producer 启动时预声明的物理 Topic。 */
    private final Set<String> topics;

    /** 客户端请求超时。 */
    private final Duration requestTimeout;

    /** RocketMQ 客户端内部最大尝试次数。 */
    private final int maxAttempts;

    /** 可选访问密钥。 */
    private final String accessKey;

    /** 可选访问密钥。 */
    private final String secretKey;


    /**
     * 校验并复制 RocketMQ 配置。
     */
    public RocketMqMessageProviderConfig(
        String endpoints,
        Set<String> topics,
        Duration requestTimeout,
        int maxAttempts,
        String accessKey,
        String secretKey) {
        // endpoints 必须存在。
        if (endpoints == null || endpoints.isBlank()) {
            // 没有 Proxy 地址无法建立客户端连接。
            throw new IllegalArgumentException("endpoints must not be blank");
        }
        // 去除地址首尾空白。
        endpoints = endpoints.trim();
        // Topic 集合不能为空。
        if (topics == null || topics.isEmpty()) {
            // gRPC Producer Builder 要求预声明发送 Topic。
            throw new IllegalArgumentException("at least one RocketMQ topic is required");
        }
        // 逐个标准化 Topic。
        LinkedHashSet<String> normalizedTopics = new LinkedHashSet<>();
        // 遍历配置 Topic。
        for (String topic : topics) {
            // Topic 不能为空。
            if (topic == null || topic.isBlank()) {
                // 空 Topic 属于启动配置错误。
                throw new IllegalArgumentException("RocketMQ topic must not be blank");
            }
            // 保存标准化 Topic。
            normalizedTopics.add(topic.trim());
        }
        // 保存不可变 Topic Set。
        topics = Set.copyOf(normalizedTopics);
        // 请求超时必须为正数。
        if (requestTimeout == null || requestTimeout.isZero() || requestTimeout.isNegative()) {
            // 非正超时无意义。
            throw new IllegalArgumentException("requestTimeout must be positive");
        }
        // 最大尝试次数至少为 1。
        if (maxAttempts < 1) {
            // 客户端必须至少尝试一次。
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        // accessKey 和 secretKey 必须同时存在或同时为空。
        boolean accessKeyPresent = accessKey != null && !accessKey.isBlank();
        // 判断 secretKey 是否存在。
        boolean secretKeyPresent = secretKey != null && !secretKey.isBlank();
        // 仅提供一个凭证字段时拒绝启动。
        if (accessKeyPresent != secretKeyPresent) {
            // 防止创建无法认证的客户端。
            throw new IllegalArgumentException(
                    "accessKey and secretKey must be configured together");
        }
        // 空白凭证统一转换为 null。
        accessKey = accessKeyPresent ? accessKey.trim() : null;
        // 空白凭证统一转换为 null。
        secretKey = secretKeyPresent ? secretKey.trim() : null;
    
        // 保存完成校验和标准化后的 endpoints。
        this.endpoints = endpoints;
        // 保存完成校验和标准化后的 topics。
        this.topics = topics;
        // 保存完成校验和标准化后的 requestTimeout。
        this.requestTimeout = requestTimeout;
        // 保存完成校验和标准化后的 maxAttempts。
        this.maxAttempts = maxAttempts;
        // 保存完成校验和标准化后的 accessKey。
        this.accessKey = accessKey;
        // 保存完成校验和标准化后的 secretKey。
        this.secretKey = secretKey;
    }

    /**
     * 创建无鉴权默认配置。
     *
     * @param endpoints gRPC Proxy 地址
     * @param topics 物理 Topic
     * @return 默认配置
     */
    public static RocketMqMessageProviderConfig defaults(
            String endpoints,
            Set<String> topics) {
        // 默认三秒请求超时和三次客户端尝试。
        return new RocketMqMessageProviderConfig(
                endpoints,
                topics,
                Duration.ofSeconds(3),
                3,
                null,
                null);
    }
    /**
     * 返回gRPC Proxy 接入地址。
     *
     * @return gRPC Proxy 接入地址
     */
    public String endpoints() {
        // 返回不可变字段。
        return endpoints;
    }

    /**
     * 返回Producer 启动时预声明的物理 Topic。
     *
     * @return Producer 启动时预声明的物理 Topic
     */
    public Set<String> topics() {
        // 返回不可变字段。
        return topics;
    }

    /**
     * 返回客户端请求超时。
     *
     * @return 客户端请求超时
     */
    public Duration requestTimeout() {
        // 返回不可变字段。
        return requestTimeout;
    }

    /**
     * 返回RocketMQ 客户端内部最大尝试次数。
     *
     * @return RocketMQ 客户端内部最大尝试次数
     */
    public int maxAttempts() {
        // 返回不可变字段。
        return maxAttempts;
    }

    /**
     * 返回可选访问密钥。
     *
     * @return 可选访问密钥
     */
    public String accessKey() {
        // 返回不可变字段。
        return accessKey;
    }

    /**
     * 返回可选访问密钥。
     *
     * @return 可选访问密钥
     */
    public String secretKey() {
        // 返回不可变字段。
        return secretKey;
    }

    /**
     * 按全部字段比较两个值对象。
     *
     * @param object 待比较对象
     * @return 字段值全部一致时返回 true
     */
    @Override
    public boolean equals(Object object) {
        // 同一对象直接相等。
        if (this == object) {
            return true;
        }
        // 类型不同或对象为空时不相等。
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        // 转换为当前类型后逐字段比较。
        RocketMqMessageProviderConfig other = (RocketMqMessageProviderConfig) object;
        return Objects.equals(endpoints, other.endpoints)
                && Objects.equals(topics, other.topics)
                && Objects.equals(requestTimeout, other.requestTimeout)
                && maxAttempts == other.maxAttempts
                && Objects.equals(accessKey, other.accessKey)
                && Objects.equals(secretKey, other.secretKey);
    }

    /**
     * 根据全部字段计算哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        // 使用与 equals 相同的字段计算哈希值。
        return Objects.hash(endpoints, topics, requestTimeout, maxAttempts, accessKey, secretKey);
    }

    /**
     * 返回便于诊断的字段摘要。
     *
     * @return 字符串摘要
     */
    @Override
    public String toString() {
        // 认证信息只输出是否配置，禁止把访问密钥写入日志。
        return "RocketMqMessageProviderConfig{" +
                "endpoints=" + endpoints +
                ", topics=" + topics +
                ", requestTimeout=" + requestTimeout +
                ", maxAttempts=" + maxAttempts +
                ", authenticationConfigured=" + (accessKey != null) +
                '}';
    }

}
