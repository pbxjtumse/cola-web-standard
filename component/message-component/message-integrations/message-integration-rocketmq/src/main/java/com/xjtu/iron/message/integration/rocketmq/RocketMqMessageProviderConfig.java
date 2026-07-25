package com.xjtu.iron.message.integration.rocketmq;

import java.time.Duration;
import java.util.Set;

/**
 * 表示 RocketMQ 5.x gRPC Java Client 的基础配置。
 *
 * @param endpoints RocketMQ Proxy 或接入端点
 * @param topics 当前 Producer 预声明的普通消息 Topic 集合
 * @param accessKey 可选访问密钥
 * @param secretKey 可选私有密钥
 * @param requestTimeout 普通 RPC 请求超时时间
 * @param maxAttempts Producer 内部最大发送尝试次数
 */
public record RocketMqMessageProviderConfig(
        String endpoints,
        Set<String> topics,
        String accessKey,
        String secretKey,
        Duration requestTimeout,
        int maxAttempts) {

    /**
     * 执行配置校验和默认值处理。
     */
    public RocketMqMessageProviderConfig {
        // 接入端点不能为空。
        if (endpoints == null || endpoints.isBlank()) {
            // RocketMQ 客户端无法在无端点时启动。
            throw new IllegalArgumentException("endpoints must not be blank");
        }
        // Topic 集合至少包含一个普通 Topic。
        if (topics == null || topics.isEmpty()) {
            // Producer 预声明 Topic 有利于启动阶段发现配置错误。
            throw new IllegalArgumentException("topics must not be empty");
        }
        // Topic 集合转换为不可变副本。
        topics = Set.copyOf(topics);
        // accessKey 与 secretKey 必须同时存在或同时缺失。
        boolean hasAccessKey = accessKey != null && !accessKey.isBlank();
        // 判断 secretKey 是否存在。
        boolean hasSecretKey = secretKey != null && !secretKey.isBlank();
        // 两者不一致属于配置错误。
        if (hasAccessKey != hasSecretKey) {
            // 禁止半配置认证信息。
            throw new IllegalArgumentException("accessKey and secretKey must be configured together");
        }
        // 未指定请求超时时使用三秒。
        requestTimeout = requestTimeout == null ? Duration.ofSeconds(3) : requestTimeout;
        // 请求超时必须为正数。
        if (requestTimeout.isZero() || requestTimeout.isNegative()) {
            // 非正超时不具备有效 RPC 语义。
            throw new IllegalArgumentException("requestTimeout must be positive");
        }
        // 最大发送尝试次数至少为一次。
        if (maxAttempts <= 0) {
            // 零次发送没有意义。
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
    }

    /**
     * 创建无认证的默认配置。
     *
     * @param endpoints RocketMQ 接入端点
     * @param topics Topic 集合
     * @return 默认配置
     */
    public static RocketMqMessageProviderConfig defaults(
            String endpoints,
            Set<String> topics) {
        // 默认请求超时三秒且只尝试一次，可靠性重试留到二期统一治理。
        return new RocketMqMessageProviderConfig(
                endpoints,
                topics,
                null,
                null,
                Duration.ofSeconds(3),
                1);
    }
}
