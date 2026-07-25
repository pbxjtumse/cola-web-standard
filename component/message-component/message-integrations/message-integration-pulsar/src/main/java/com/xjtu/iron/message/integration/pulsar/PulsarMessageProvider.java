package com.xjtu.iron.message.integration.pulsar;

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
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.client.api.TypedMessageBuilder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 Pulsar 稳定 Java Client 的基础消息 Provider。
 */
public final class PulsarMessageProvider implements MessageProvider {

    /** Provider 对外稳定名称。 */
    public static final String NAME = "pulsar";

    /** Pulsar Provider 基础配置。 */
    private final PulsarMessageProviderConfig config;

    /** 可复用 PulsarClient。 */
    private final PulsarClient client;

    /** 按 Topic 缓存线程安全 Producer。 */
    private final ConcurrentMap<String, Producer<byte[]>> producers = new ConcurrentHashMap<>();

    /** 保存当前 Provider 创建的 Consumer。 */
    private final ConcurrentMap<String, Consumer<byte[]>> consumers = new ConcurrentHashMap<>();

    /** 标记 Provider 是否已经关闭。 */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * 创建 Pulsar Provider。
     *
     * @param config Pulsar 基础配置
     */
    public PulsarMessageProvider(PulsarMessageProviderConfig config) {
        // 配置不能为空。
        this.config = java.util.Objects.requireNonNull(config, "config must not be null");
        // 创建 PulsarClient。
        try {
            // 设置服务地址和操作超时。
            this.client = PulsarClient.builder()
                    .serviceUrl(config.serviceUrl())
                    .operationTimeout((int) config.operationTimeout().toMillis(), TimeUnit.MILLISECONDS)
                    .build();
        } catch (PulsarClientException exception) {
            // 客户端创建失败属于启动错误。
            throw new IllegalStateException("failed to create Pulsar client", exception);
        }
    }

    /**
     * 返回 Pulsar Provider 名称。
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
        // 事务、Key_Shared 和 Reader 等能力留到三期。
        return Set.of(
                MessageCapability.BASIC_PUBLISH,
                MessageCapability.BASIC_CONSUME);
    }

    /**
     * 使用 Pulsar Producer 异步发送消息。
     */
    @Override
    public CompletionStage<ProviderSendResult> send(ProviderSendRequest request) {
        // Provider 关闭后返回明确失败。
        if (closed.get()) {
            // 不再创建或使用 Producer。
            return CompletableFuture.completedFuture(ProviderSendResult.of(
                    SendStatus.FAILED,
                    SendFailureType.CLIENT_ERROR,
                    "Pulsar provider is closed"));
        }
        // 获取或创建当前 Topic 对应 Producer。
        Producer<byte[]> producer;
        // Producer 创建可能失败。
        try {
            // 使用按 Topic 缓存，避免每条消息创建网络资源。
            producer = producers.computeIfAbsent(
                    request.destination().logicalName(),
                    this::createProducerUnchecked);
        } catch (RuntimeException exception) {
            // 解包并标准化创建失败。
            return CompletableFuture.completedFuture(
                    classifySendFailure(exception));
        }
        // 创建一条字节消息。
        TypedMessageBuilder<byte[]> builder = producer.newMessage()
                .value(request.payload());
        // 有消息键时设置 Pulsar Key。
        if (request.key() != null && !request.key().isBlank()) {
            // Shared 订阅暂不承诺同 Key 顺序，Key_Shared 留到三期。
            builder.key(request.key());
        }
        // 将统一消息头写入 Pulsar Properties。
        request.headers().forEach(builder::property);
        // 将 Provider 扩展属性同样写入 Properties。
        request.providerProperties().forEach(builder::property);
        // 创建由 Pulsar Future 回调完成的标准结果 Future。
        CompletableFuture<ProviderSendResult> resultFuture = new CompletableFuture<>();
        // 发起异步发送。
        builder.sendAsync().whenComplete((messageId, throwable) -> {
            // 异常分支执行统一分类。
            if (throwable != null) {
                // 完成失败、拒绝或不确定结果。
                resultFuture.complete(classifySendFailure(throwable));
                // 结束异常分支。
                return;
            }
            // Pulsar MessageId 可转换为稳定诊断字符串。
            resultFuture.complete(
                    ProviderSendResult.confirmed(messageId.toString()));
        });
        // 返回异步标准结果。
        return resultFuture;
    }

    /**
     * 创建 Shared Subscription 消费者。
     */
    @Override
    public MessageConsumer subscribe(ProviderSubscription subscription) {
        // Provider 关闭后不允许新增消费者。
        if (closed.get()) {
            // 消费者资源无法启动时直接失败。
            throw new IllegalStateException("Pulsar provider is closed");
        }
        // 创建原生 Consumer。
        Consumer<byte[]> consumer;
        // 捕获订阅创建失败。
        try {
            // 第一版使用 Shared 订阅表达消费组内负载均衡。
            consumer = client.newConsumer(Schema.BYTES)
                    .topic(subscription.destination().logicalName())
                    .subscriptionName(subscription.consumerGroup())
                    .subscriptionType(SubscriptionType.Shared)
                    .receiverQueueSize(config.receiverQueueSize())
                    .messageListener((nativeConsumer, message) -> {
                        // 将 Pulsar Message 转换为统一入站消息。
                        ProviderInboundMessage inboundMessage = toInbound(
                                subscription,
                                message);
                        // 默认按 RETRY 处理。
                        ConsumeDecision decision = ConsumeDecision.RETRY;
                        // 调用 core 监听器。
                        try {
                            // 获取业务消费决策。
                            decision = subscription.listener().onMessage(inboundMessage);
                        } catch (RuntimeException ignored) {
                            // 异常保持 RETRY。
                        }
                        // SUCCESS 时异步 ACK。
                        if (decision == ConsumeDecision.SUCCESS) {
                            // Pulsar ACK 失败会触发后续重新投递。
                            nativeConsumer.acknowledgeAsync(message);
                            // 成功分支结束。
                            return;
                        }
                        // RETRY 时发送 Negative Acknowledgement。
                        nativeConsumer.negativeAcknowledge(message);
                    })
                    .subscribe();
        } catch (PulsarClientException exception) {
            // 启动失败包装为非法状态异常。
            throw new IllegalStateException("failed to create Pulsar consumer", exception);
        }
        // 创建内部 Consumer 标识。
        String consumerId = UUID.randomUUID().toString();
        // 保存 Consumer 便于 Provider 统一关闭。
        consumers.put(consumerId, consumer);
        // 返回统一关闭句柄。
        return () -> closeConsumer(consumerId, consumer);
    }

    /**
     * 关闭全部 Consumer、Producer 和 PulsarClient。
     */
    @Override
    public void close() {
        // 仅第一次调用执行关闭。
        if (closed.compareAndSet(false, true)) {
            // 逐个关闭 Consumer。
            consumers.forEach(this::closeConsumer);
            // 清空 Consumer 注册表。
            consumers.clear();
            // 逐个关闭缓存 Producer。
            producers.values().forEach(producer -> {
                // 每个 Producer 独立关闭，单个失败不影响其他资源。
                try {
                    // 释放 Producer 资源。
                    producer.close();
                } catch (PulsarClientException ignored) {
                    // 第一版不引入日志门面。
                }
            });
            // 清空 Producer 缓存。
            producers.clear();
            // 最后关闭共享 PulsarClient。
            try {
                // 释放底层连接和线程池。
                client.close();
            } catch (PulsarClientException ignored) {
                // 保持 close 幂等且不阻止应用退出。
            }
        }
    }

    /**
     * 创建一个 Topic 对应的 Producer，并把受检异常转换为运行时异常。
     */
    private Producer<byte[]> createProducerUnchecked(String topic) {
        // 创建 Producer 可能抛出 PulsarClientException。
        try {
            // 使用 BYTES Schema，序列化由 message-core 统一完成。
            return client.newProducer(Schema.BYTES)
                    .topic(topic)
                    .create();
        } catch (PulsarClientException exception) {
            // computeIfAbsent 不支持受检异常，因此包装后由 send 统一转换。
            throw new IllegalStateException("failed to create Pulsar producer for topic " + topic, exception);
        }
    }

    /**
     * 将 Pulsar Message 转换为统一入站消息。
     */
    private static ProviderInboundMessage toInbound(
            ProviderSubscription subscription,
            Message<byte[]> message) {
        // 复制 Pulsar Properties。
        Map<String, String> headers = new LinkedHashMap<>(message.getProperties());
        // 仅在消息实际包含 Key 时读取。
        String key = message.hasKey() ? message.getKey() : null;
        // redeliveryCount 从零开始，因此统一投递次数需要加一。
        int deliveryAttempt = message.getRedeliveryCount() + 1;
        // 创建统一入站消息。
        return new ProviderInboundMessage(
                subscription.destination(),
                message.getMessageId().toString(),
                key,
                headers,
                message.getData(),
                deliveryAttempt);
    }

    /**
     * 关闭并移除一个 Pulsar Consumer。
     */
    private void closeConsumer(String consumerId, Consumer<byte[]> consumer) {
        // 从注册表移除当前实例。
        consumers.remove(consumerId, consumer);
        // 关闭 Consumer。
        try {
            // 释放订阅和网络资源。
            consumer.close();
        } catch (PulsarClientException ignored) {
            // 单个消费者关闭失败不阻止其他资源释放。
        }
    }

    /**
     * 将 Pulsar 异常转换为 Provider 标准结果。
     */
    private static ProviderSendResult classifySendFailure(Throwable throwable) {
        // 逐层解包 CompletionException 或包装异常。
        Throwable actual = unwrap(throwable);
        // 根据类型名称执行保守分类，兼容不同小版本异常层级。
        String typeName = actual.getClass().getSimpleName().toLowerCase();
        // 超时无法确定 Broker 是否已经持久化消息。
        if (typeName.contains("timeout")) {
            // 返回 UNKNOWN。
            return ProviderSendResult.of(
                    SendStatus.UNKNOWN,
                    SendFailureType.TIMEOUT,
                    actual.getMessage());
        }
        // 认证或授权失败属于明确拒绝。
        if (typeName.contains("auth") || typeName.contains("authorization")) {
            // 返回 REJECTED。
            return ProviderSendResult.of(
                    SendStatus.REJECTED,
                    SendFailureType.AUTHENTICATION_ERROR,
                    actual.getMessage());
        }
        // 其他异常按客户端失败处理。
        return ProviderSendResult.of(
                SendStatus.FAILED,
                SendFailureType.CLIENT_ERROR,
                actual.getMessage());
    }

    /**
     * 解包常见异步包装异常。
     */
    private static Throwable unwrap(Throwable throwable) {
        // 从原始异常开始。
        Throwable actual = throwable;
        // 包装异常存在 cause 时持续下钻。
        while (actual.getCause() != null
                && (actual instanceof java.util.concurrent.CompletionException
                    || actual instanceof java.util.concurrent.ExecutionException
                    || actual instanceof IllegalStateException)) {
            // 切换到下一层 cause。
            actual = actual.getCause();
        }
        // 返回真实异常。
        return actual;
    }
}
