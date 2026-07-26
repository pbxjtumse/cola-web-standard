package com.xjtu.iron.message.integration.rocketmq;

import com.xjtu.iron.message.api.ConsumeDecision;
import com.xjtu.iron.message.api.SendFailureType;
import com.xjtu.iron.message.api.SendStatus;
import com.xjtu.iron.message.spi.MessageCapability;
import com.xjtu.iron.message.spi.MessageProvider;
import com.xjtu.iron.message.spi.ProviderInboundMessage;
import com.xjtu.iron.message.spi.ProviderSendRequest;
import com.xjtu.iron.message.spi.ProviderSendResult;
import com.xjtu.iron.message.spi.ProviderSubscription;
import com.xjtu.iron.message.spi.ProviderSubscriptionRequest;
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
import java.time.Instant;
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
 * 基于 RocketMQ 5.x gRPC Java Client 的一期普通消息 Provider。
 */
public final class RocketMqMessageProvider implements MessageProvider {

    /** Provider 稳定名称。 */
    public static final String NAME = "rocketmq";

    /** RocketMQ 客户端服务工厂。 */
    private final ClientServiceProvider serviceProvider;

    /** 可复用客户端配置。 */
    private final ClientConfiguration clientConfiguration;

    /** 普通消息 Producer。 */
    private final Producer producer;

    /** Producer 启动时预声明的物理 Topic。 */
    private final Set<String> topics;

    /** 当前 Provider 创建的全部 PushConsumer。 */
    private final ConcurrentMap<String, PushConsumer> consumers =
            new ConcurrentHashMap<>();

    /** Provider 关闭状态。 */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * 创建 RocketMQ Provider。
     *
     * @param config RocketMQ 配置
     */
    public RocketMqMessageProvider(RocketMqMessageProviderConfig config) {
        // 配置不能为空。
        java.util.Objects.requireNonNull(config, "config must not be null");
        // 保存预声明 Topic。
        this.topics = config.topics();
        // 加载 RocketMQ Client ServiceProvider。
        this.serviceProvider = ClientServiceProvider.loadService();
        // 创建客户端配置 Builder。
        ClientConfigurationBuilder clientBuilder = ClientConfiguration.newBuilder()
                // 设置 gRPC Proxy 地址。
                .setEndpoints(config.endpoints())
                // 设置请求超时。
                .setRequestTimeout(config.requestTimeout());
        // 仅在完整配置凭证时设置认证。
        if (config.accessKey() != null) {
            // 创建静态会话凭证。
            StaticSessionCredentialsProvider credentialsProvider =
                    new StaticSessionCredentialsProvider(
                            config.accessKey(),
                            config.secretKey());
            // 将凭证设置到客户端配置。
            clientBuilder.setCredentialProvider(credentialsProvider);
        }
        // 构建不可变客户端配置。
        this.clientConfiguration = clientBuilder.build();
        // 创建 Producer。
        try {
            // 配置客户端、Topic 和最大尝试次数。
            this.producer = serviceProvider.newProducerBuilder()
                    .setClientConfiguration(clientConfiguration)
                    .setTopics(config.topics().toArray(String[]::new))
                    .setMaxAttempts(config.maxAttempts())
                    .build();
        } catch (ClientException exception) {
            // Producer 创建失败属于启动错误。
            throw new IllegalStateException("failed to create RocketMQ producer", exception);
        }
    }

    /**
     * 返回 Provider 名称。
     */
    @Override
    public String name() {
        // 返回稳定小写名称。
        return NAME;
    }

    /**
     * 返回一期公共能力。
     */
    @Override
    public Set<MessageCapability> capabilities() {
        // 声明普通发布和普通消费。
        return Set.of(
                MessageCapability.BASIC_PUBLISH,
                MessageCapability.BASIC_CONSUME);
    }

    /**
     * 异步发送普通 RocketMQ 消息。
     */
    @Override
    public CompletionStage<ProviderSendResult> send(ProviderSendRequest request) {
        // Provider 关闭后返回明确本地失败。
        if (closed.get()) {
            // 不调用已关闭 Producer。
            return CompletableFuture.completedFuture(ProviderSendResult.failed(
                    SendStatus.FAILED,
                    SendFailureType.CLIENT_ERROR,
                    "RocketMQ provider is closed"));
        }
        // gRPC Producer 只能发送构造时声明的 Topic。
        if (!topics.contains(request.destination().physicalName())) {
            // 未声明 Topic 属于明确配置拒绝。
            return CompletableFuture.completedFuture(ProviderSendResult.failed(
                    SendStatus.REJECTED,
                    SendFailureType.ROUTING_ERROR,
                    "RocketMQ topic was not declared: "
                            + request.destination().physicalName()));
        }
        // 创建 RocketMQ Message Builder。
        MessageBuilder messageBuilder = serviceProvider.newMessageBuilder()
                // 设置物理 Topic。
                .setTopic(request.destination().physicalName())
                // 设置已序列化消息体。
                .setBody(request.body());
        // key 存在时写入 RocketMQ Keys。
        if (request.key() != null && !request.key().isBlank()) {
            // 原生 Key 便于定位消息和后续高级能力演进。
            messageBuilder.setKeys(request.key());
        }
        // 将完整线级消息头写入 RocketMQ 用户属性。
        request.headers().forEach(messageBuilder::addProperty);
        // 构造不可变 RocketMQ Message。
        Message message = messageBuilder.build();
        // 创建公共结果 Future。
        CompletableFuture<ProviderSendResult> resultFuture = new CompletableFuture<>();
        // 发起异步发送。
        producer.sendAsync(message).whenComplete((receipt, throwable) -> {
            // 异常分支进行保守分类。
            if (throwable != null) {
                // 完成非成功结果。
                resultFuture.complete(classifySendFailure(throwable));
                // 结束回调。
                return;
            }
            // 获取 RocketMQ 原生 MessageId。
            String providerMessageId = receipt.getMessageId().toString();
            // 收到 SendReceipt 后标记明确成功。
            resultFuture.complete(ProviderSendResult.confirmed(providerMessageId));
        });
        // 返回公共 Future。
        return resultFuture;
    }

    /**
     * 创建并启动 RocketMQ PushConsumer。
     */
    @Override
    public ProviderSubscription subscribe(ProviderSubscriptionRequest request) {
        // Provider 关闭后拒绝新订阅。
        if (closed.get()) {
            // 启动阶段直接失败。
            throw new IllegalStateException("RocketMQ provider is closed");
        }
        // 一期只使用全量 Tag 过滤表达式。
        FilterExpression filterExpression = new FilterExpression(
                "*",
                FilterExpressionType.TAG);
        // 一个订阅对应一个物理 Topic。
        Map<String, FilterExpression> expressions = Collections.singletonMap(
                request.destination().physicalName(),
                filterExpression);
        // 创建 PushConsumer。
        PushConsumer consumer;
        // 捕获客户端启动异常。
        try {
            // 配置客户端、消费组、订阅和消息监听器。
            consumer = serviceProvider.newPushConsumerBuilder()
                    .setClientConfiguration(clientConfiguration)
                    .setConsumerGroup(request.consumerGroup())
                    .setSubscriptionExpressions(expressions)
                    .setMessageListener(messageView -> {
                        // 转换原生消息。
                        ProviderInboundMessage inbound = toInbound(messageView);
                        // 默认 RETRY 防止异常误确认。
                        ConsumeDecision decision = ConsumeDecision.RETRY;
                        // 调用 core 监听器。
                        try {
                            // 获取业务决策。
                            decision = request.listener().onMessage(inbound);
                        } catch (RuntimeException ignored) {
                            // 异常保持 RETRY。
                        }
                        // 映射为 RocketMQ ConsumeResult。
                        return decision == ConsumeDecision.SUCCESS
                                ? ConsumeResult.SUCCESS
                                : ConsumeResult.FAILURE;
                    })
                    .build();
        } catch (ClientException exception) {
            // Consumer 启动失败属于明确启动错误。
            throw new IllegalStateException("failed to create RocketMQ consumer", exception);
        }
        // 创建内部 Consumer ID。
        String consumerId = UUID.randomUUID().toString();
        // 保存 Consumer 以支持 Provider 统一关闭。
        consumers.put(consumerId, consumer);
        // 返回关闭句柄。
        return () -> closeConsumer(consumerId, consumer);
    }

    /**
     * 关闭 Producer 和全部 PushConsumer。
     */
    @Override
    public void close() {
        // 只允许第一次关闭释放资源。
        if (closed.compareAndSet(false, true)) {
            // 逐个关闭 Consumer。
            consumers.forEach(this::closeConsumer);
            // 清空注册表。
            consumers.clear();
            // 关闭 Producer。
            try {
                // Producer.close 可能抛出异常。
                producer.close();
            } catch (Exception ignored) {
                // 一期没有生命周期日志，继续完成其他资源释放。
            }
        }
    }

    /**
     * 转换 RocketMQ MessageView。
     */
    private static ProviderInboundMessage toInbound(MessageView messageView) {
        // 获取消息体 ByteBuffer。
        ByteBuffer bodyBuffer = messageView.getBody();
        // 创建剩余长度数组。
        byte[] body = new byte[bodyBuffer.remaining()];
        // 复制消息体。
        bodyBuffer.get(body);
        // RocketMQ 支持多个 Key，公共模型使用第一个。
        String key = messageView.getKeys().stream().findFirst().orElse(null);
        // 复制用户属性为线级消息头。
        Map<String, String> headers = new LinkedHashMap<>(messageView.getProperties());
        // 构造诊断元数据。
        Map<String, String> metadata = Map.of(
                "deliveryAttempt",
                Integer.toString(messageView.getDeliveryAttempt()));
        // 创建统一入站消息。
        return new ProviderInboundMessage(
                messageView.getMessageId().toString(),
                key,
                headers,
                body,
                messageView.getDeliveryAttempt(),
                Instant.now(),
                metadata);
    }

    /**
     * 关闭并移除一个 PushConsumer。
     */
    private void closeConsumer(String consumerId, PushConsumer consumer) {
        // 仅当注册表仍映射当前实例时移除。
        consumers.remove(consumerId, consumer);
        // 关闭原生 Consumer。
        try {
            // 释放网络和线程资源。
            consumer.close();
        } catch (Exception ignored) {
            // 保持其他资源继续关闭。
        }
    }

    /**
     * 分类 RocketMQ 发送异常。
     */
    private static ProviderSendResult classifySendFailure(Throwable throwable) {
        // 使用异常简单类名进行兼容性较强的保守分类。
        String typeName = throwable.getClass().getSimpleName().toLowerCase();
        // 超时无法证明 Broker 未接收消息。
        if (typeName.contains("timeout")) {
            // 返回 UNKNOWN。
            return ProviderSendResult.failed(
                    SendStatus.UNKNOWN,
                    SendFailureType.TIMEOUT,
                    throwable.getMessage());
        }
        // 认证失败属于明确拒绝。
        if (typeName.contains("authentication") || typeName.contains("credential")) {
            // 返回认证错误。
            return ProviderSendResult.failed(
                    SendStatus.REJECTED,
                    SendFailureType.AUTHENTICATION_ERROR,
                    throwable.getMessage());
        }
        // 授权或权限失败属于明确拒绝。
        if (typeName.contains("authorization") || typeName.contains("permission")) {
            // 返回权限错误。
            return ProviderSendResult.failed(
                    SendStatus.REJECTED,
                    SendFailureType.AUTHORIZATION_ERROR,
                    throwable.getMessage());
        }
        // 网络相关异常采用 UNKNOWN，避免无条件重发。
        if (typeName.contains("network")
                || typeName.contains("connect")
                || typeName.contains("remoting")) {
            // 返回网络不确定状态。
            return ProviderSendResult.failed(
                    SendStatus.UNKNOWN,
                    SendFailureType.NETWORK_ERROR,
                    throwable.getMessage());
        }
        // 其他异常按客户端明确失败处理。
        return ProviderSendResult.failed(
                SendStatus.FAILED,
                SendFailureType.CLIENT_ERROR,
                throwable.getMessage());
    }
}
