package com.xjtu.iron.message.integration.rocketmq;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 表示 RocketMQ 5.x gRPC Java Client 的一期基础配置。
 *
 * @param endpoints gRPC Proxy 接入地址
 * @param topics Producer 启动时预声明的物理 Topic
 * @param requestTimeout 客户端请求超时
 * @param maxAttempts RocketMQ 客户端内部最大尝试次数
 * @param accessKey 可选访问密钥
 * @param secretKey 可选访问密钥
 */
public record RocketMqMessageProviderConfig(
        String endpoints,
        Set<String> topics,
        Duration requestTimeout,
        int maxAttempts,
        String accessKey,
        String secretKey) {

    /**
     * 校验并复制 RocketMQ 配置。
     */
    public RocketMqMessageProviderConfig {
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
}
