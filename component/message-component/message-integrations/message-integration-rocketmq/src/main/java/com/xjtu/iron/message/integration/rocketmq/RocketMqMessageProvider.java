package com.xjtu.iron.message.integration.rocketmq;

import com.xjtu.iron.message.api.ConsumeDecision;
import com.xjtu.iron.message.api.MessageConsumer;
import com.xjtu.iron.message.api.SendFailureType;
import com.xjtu.iron.message.api.SendStatus;
import com.xjtu.iron.message.api.spi.MessageCapability;
import com.xjtu.iron.message.api.spi.MessageProvider;
import com.xjtu.iron.message.api.spi.ProviderInboundMessage;
import com.xjtu.iron.message.api.spi.ProviderSendRequest;
import com.xjtu.iron.message.api.spi.ProviderSendResult;
import com.xjtu.iron.message.api.spi.ProviderSubscription;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientConfigurationBuilder;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.StaticSessionCredentialsProvider;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.message.MessageBuilder;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.rocketmq.client.apis.producer.Producer;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 RocketMQ 5.x gRPC Java Client 的基础消息 Provider。
 */
public final class RocketMqMessageProvider implements MessageProvider {

    /** Provider 对外稳定名称。 */
    public static final String NAME = "rocketmq";

    /** RocketMQ 客户端服务工厂。 */
    private final ClientServiceProvider serviceProvider;

    /** 可复用客户端配置。 */
    private final ClientConfiguration clientConfiguration;

    /** 可复用普通消息 Producer。 */
    private final Producer producer;

    /** 启动时预声明的 Topic 集合。 */
    private final Set<String> topics;

    /** 保存全部 PushConsumer 关闭句柄。 */
    private final ConcurrentMap<String, PushConsumer> consumers = new ConcurrentHashMap<>();

    /** 标记 Provider 是否已经关闭。 */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * 创建 RocketMQ Provider。
     *
     * @param config RocketMQ 基础配置
     */
    public RocketMqMessageProvider(RocketMqMessageProviderConfig config) {
        // 配置不能为空。
        java.util.Objects.requireNonNull(config, "config must not be null");
        // 保存预声明 Topic。
        this.topics = config.topics();
        // 加载 RocketMQ Java Client 服务实现。
        this.serviceProvider = ClientServiceProvider.loadService();
        // 创建 ClientConfiguration Builder。
        ClientConfigurationBuilder clientBuilder = ClientConfiguration.newBuilder()
                .setEndpoints(config.endpoints())
                .setRequestTimeout(config.requestTimeout());
        // 仅在认证信息完整时设置凭证。
        if (config.accessKey() != null && !config.accessKey().isBlank()) {
            // 创建静态会话凭证。
            StaticSessionCredentialsProvider credentialsProvider =
                    new StaticSessionCredentialsProvider(
                            config.accessKey(),
                            config.secretKey());
            // 将凭证应用于客户端配置。
            clientBuilder.setCredentialProvider(credentialsProvider);
        }
        // 构建不可变客户端配置。
        this.clientConfiguration = clientBuilder.build();
        // 创建并启动普通消息 Producer。
        try {
            // 预声明 Topic 并设置内部尝试次数。
            this.producer = serviceProvider.newProducerBuilder()
                    .setClientConfiguration(clientConfiguration)
                    .setTopics(config.topics().toArray(String[]::new))
                    .setMaxAttempts(config.maxAttempts())
                    .build();
        } catch (ClientException exception) {
            // Provider 构造失败属于启动错误，包装为非法状态异常。
            throw new IllegalStateException("failed to create RocketMQ producer", exception);
        }
    }

    /**
     * 返回 RocketMQ Provider 名称。
     */
    @Override
    public String name() {
        // 返回稳定小写名称。
        return NAME;
    }

    /**
     * 返回基础发布和消费能力。
     */
    @Override
    public Set<MessageCapability> capabilities() {
        // 高级 FIFO、延时和事务能力不在第一版公共能力中声明。
        return Set.of(
                MessageCapability.BASIC_PUBLISH,
                MessageCapability.BASIC_CONSUME);
    }

    /**
     * 使用 RocketMQ Producer 异步发送普通消息。
     */
    @Override
    public CompletionStage<ProviderSendResult> send(ProviderSendRequest request) {
        // Provider 已关闭时返回明确失败。
        if (closed.get()) {
            // 通过标准结果反馈资源状态。
            return CompletableFuture.completedFuture(ProviderSendResult.of(
                    SendStatus.FAILED,
                    SendFailureType.CLIENT_ERROR,
                    "RocketMQ provider is closed"));
        }
        // 第一版要求目标 Topic 已在 Producer 启动时声明。
        if (!topics.contains(request.destination().logicalName())) {
            // 未声明 Topic 作为配置拒绝处理。
            return CompletableFuture.completedFuture(ProviderSendResult.of(
                    SendStatus.REJECTED,
                    SendFailureType.VALIDATION_ERROR,
                    "RocketMQ topic was not declared: " + request.destination().logicalName()));
        }
        // 创建 RocketMQ 消息 Builder。
        MessageBuilder messageBuilder = serviceProvider.newMessageBuilder()
                .setTopic(request.destination().logicalName())
                .setBody(request.payload());
        // 有业务键时同时写入 RocketMQ 原生 Key。
        if (request.key() != null && !request.key().isBlank()) {
            // 原生 Key 有利于 Broker 管理和问题定位。
            messageBuilder.setKeys(request.key());
        }
        // 将统一消息头复制为 RocketMQ 用户属性。
        request.headers().forEach(messageBuilder::addProperty);
        // 将 Provider 扩展属性也复制为用户属性。
        request.providerProperties().forEach(messageBuilder::addProperty);
        // 构建不可变 RocketMQ Message。
        Message message = messageBuilder.build();
        // 创建由 sendAsync 回调完成的标准 Future。
        CompletableFuture<ProviderSendResult> resultFuture = new CompletableFuture<>();
        // 发起异步发送。
        producer.sendAsync(message).whenComplete((receipt, throwable) -> {
            // 异常分支执行统一分类。
            if (throwable != null) {
                // 完成失败、拒绝或不确定结果。
                resultFuture.complete(classifySendFailure(throwable));
                // 结束异常分支。
                return;
            }
            // RocketMQ SendReceipt 提供原生 MessageId。
            String nativeMessageId = receipt.getMessageId().toString();
            // 收到发送回执后返回明确确认。
            resultFuture.complete(ProviderSendResult.confirmed(nativeMessageId));
        });
        // 返回异步标准结果。
        return resultFuture;
    }

    /**
     * 创建 RocketMQ PushConsumer。
     */
    @Override
    public MessageConsumer subscribe(ProviderSubscription subscription) {
        // Provider 关闭后不允许创建消费者。
        if (closed.get()) {
            // 消费者资源无法启动时直接失败。
            throw new IllegalStateException("RocketMQ provider is closed");
        }
        // 第一版只使用全量 Tag 过滤表达式。
        FilterExpression filterExpression = new FilterExpression(
                "*",
                FilterExpressionType.TAG);
        // 一个基础消费者只订阅一个逻辑 Topic。
        Map<String, FilterExpression> expressions = Collections.singletonMap(
                subscription.destination().logicalName(),
                filterExpression);
        // 创建并启动 PushConsumer。
        PushConsumer consumer;
        // 捕获 RocketMQ 客户端启动异常。
        try {
            // 配置客户端、消费组、订阅和监听器。
            consumer = serviceProvider.newPushConsumerBuilder()
                    .setClientConfiguration(clientConfiguration)
                    .setConsumerGroup(subscription.consumerGroup())
                    .setSubscriptionExpressions(expressions)
                    .setMessageListener(messageView -> {
                        // 将 RocketMQ MessageView 转为统一入站消息。
                        ProviderInboundMessage inboundMessage = toInbound(
                                subscription,
                                messageView);
                        // 默认使用 RETRY，避免监听器异常被误 ACK。
                        ConsumeDecision decision = ConsumeDecision.RETRY;
                        // 调用 core 监听器。
                        try {
                            // 获取业务消费决策。
                            decision = subscription.listener().onMessage(inboundMessage);
                        } catch (RuntimeException ignored) {
                            // 异常保持 RETRY。
                        }
                        // SUCCESS 翻译为 RocketMQ SUCCESS。
                        return decision == ConsumeDecision.SUCCESS
                                ? ConsumeResult.SUCCESS
                                : ConsumeResult.FAILURE;
                    })
                    .build();
        } catch (ClientException exception) {
            // 启动失败包装为非法状态异常。
            throw new IllegalStateException("failed to create RocketMQ consumer", exception);
        }
        // 为关闭管理创建内部标识。
        String consumerId = UUID.randomUUID().toString();
        // 保存 Consumer。
        consumers.put(consumerId, consumer);
        // 返回统一关闭句柄。
        return () -> closeConsumer(consumerId, consumer);
    }

    /**
     * 关闭 Producer 和全部 PushConsumer。
     */
    @Override
    public void close() {
        // 只执行一次资源释放。
        if (closed.compareAndSet(false, true)) {
            // 逐个关闭 PushConsumer。
            consumers.forEach(this::closeConsumer);
            // 清空注册表。
            consumers.clear();
            // 关闭 Producer。
            try {
                // RocketMQ Producer.close 可能抛出受检异常。
                producer.close();
            } catch (Exception ignored) {
                // 第一版没有日志集成；二期通过生命周期事件记录关闭失败。
            }
        }
    }

    /**
     * 把 RocketMQ MessageView 转换为统一入站消息。
     */
    private static ProviderInboundMessage toInbound(
            ProviderSubscription subscription,
            MessageView messageView) {
        // 获取 RocketMQ 提供的消息体副本。
        ByteBuffer bodyBuffer = messageView.getBody();
        // 根据剩余长度创建字节数组。
        byte[] payload = new byte[bodyBuffer.remaining()];
        // 复制消息体。
        bodyBuffer.get(payload);
        // 获取第一个原生消息 Key；不存在时为 null。
        String key = messageView.getKeys().stream().findFirst().orElse(null);
        // 复制用户属性，避免后续修改原生对象。
        Map<String, String> headers = new LinkedHashMap<>(messageView.getProperties());
        // 创建统一入站消息。
        return new ProviderInboundMessage(
                subscription.destination(),
                messageView.getMessageId().toString(),
                key,
                headers,
                payload,
                messageView.getDeliveryAttempt());
    }

    /**
     * 关闭并移除一个 PushConsumer。
     */
    private void closeConsumer(String consumerId, PushConsumer consumer) {
        // 仅当注册表仍映射到当前实例时移除。
        consumers.remove(consumerId, consumer);
        // 关闭原生 Consumer。
        try {
            // 释放网络连接和线程资源。
            consumer.close();
        } catch (Exception ignored) {
            // 第一版保持其他资源继续关闭。
        }
    }

    /**
     * 将 RocketMQ 发送异常转换为 Provider 标准结果。
     */
    private static ProviderSendResult classifySendFailure(Throwable throwable) {
        // 根据异常类型名称进行保守分类，避免绑定内部实现异常层级。
        String typeName = throwable.getClass().getSimpleName().toLowerCase();
        // 超时无法判断 Broker 是否已经接收消息。
        if (typeName.contains("timeout")) {
            // 返回 UNKNOWN 而不是确定失败。
            return ProviderSendResult.of(
                    SendStatus.UNKNOWN,
                    SendFailureType.TIMEOUT,
                    throwable.getMessage());
        }
        // 认证或权限错误属于明确拒绝。
        if (typeName.contains("auth") || typeName.contains("permission")) {
            // 返回 REJECTED。
            return ProviderSendResult.of(
                    SendStatus.REJECTED,
                    SendFailureType.AUTHENTICATION_ERROR,
                    throwable.getMessage());
        }
        // 其他异常按客户端失败处理。
        return ProviderSendResult.of(
                SendStatus.FAILED,
                SendFailureType.CLIENT_ERROR,
                throwable.getMessage());
    }
}
