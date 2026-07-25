package com.xjtu.iron.message.integration.kafka;

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
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 Kafka 原生 Java Client 的基础消息 Provider。
 *
 * <p>第一版使用手动提交 offset：业务 SUCCESS 后提交当前记录的下一个 offset，
 * 业务 RETRY 时 seek 回当前 offset 并短暂退避。</p>
 */
public final class KafkaMessageProvider implements MessageProvider {

    /** Provider 对外稳定名称。 */
    public static final String NAME = "kafka";

    /** Kafka Provider 基础配置。 */
    private final KafkaMessageProviderConfig config;

    /** 线程安全的 Kafka Producer。 */
    private final Producer<String, byte[]> producer;

    /** 保存当前 Provider 创建的全部 Consumer Worker。 */
    private final ConcurrentMap<String, KafkaConsumerWorker> workers = new ConcurrentHashMap<>();

    /** 标记 Provider 是否已经关闭。 */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * 创建 Kafka Provider 并初始化 Producer。
     *
     * @param config Kafka 基础配置
     */
    public KafkaMessageProvider(KafkaMessageProviderConfig config) {
        // 配置不能为空。
        this.config = java.util.Objects.requireNonNull(config, "config must not be null");
        // 创建 Kafka Producer 原生配置。
        Map<String, Object> properties = new LinkedHashMap<>();
        // 设置 Broker 地址。
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers());
        // 设置客户端标识。
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, config.clientId());
        // 消息键统一使用 String。
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // 消息体在 core 已经序列化为 byte[]。
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        // 第一版要求获得全部同步副本确认后再报告成功。
        properties.putIfAbsent(ProducerConfig.ACKS_CONFIG, "all");
        // 允许调用方显式覆盖或增加 Kafka 原生配置。
        properties.putAll(config.producerProperties());
        // 创建可复用 Producer。
        this.producer = new KafkaProducer<>(properties);
    }

    /**
     * 返回 Kafka Provider 名称。
     */
    @Override
    public String name() {
        // 返回稳定小写名称。
        return NAME;
    }

    /**
     * 返回基础发布与消费能力。
     */
    @Override
    public Set<MessageCapability> capabilities() {
        // 第一期只声明两项公共能力。
        return Set.of(
                MessageCapability.BASIC_PUBLISH,
                MessageCapability.BASIC_CONSUME);
    }

    /**
     * 使用 Kafka Producer 异步发送消息。
     */
    @Override
    public CompletionStage<ProviderSendResult> send(ProviderSendRequest request) {
        // Provider 关闭后返回明确失败。
        if (closed.get()) {
            // 通过标准结果表达状态。
            return CompletableFuture.completedFuture(ProviderSendResult.of(
                    SendStatus.FAILED,
                    SendFailureType.CLIENT_ERROR,
                    "Kafka provider is closed"));
        }
        // 创建 Kafka 原生记录；第一版逻辑目的地直接映射 Topic。
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(
                request.destination().logicalName(),
                request.key(),
                request.payload());
        // 将统一消息头复制到 Kafka Headers。
        request.headers().forEach((key, value) -> {
            // Kafka Header 值是 byte[]，统一使用 UTF-8 编码。
            record.headers().add(key, value.getBytes(StandardCharsets.UTF_8));
        });
        // 创建由回调完成的标准结果 Future。
        CompletableFuture<ProviderSendResult> resultFuture = new CompletableFuture<>();
        // 调用 Kafka 异步发送。
        producer.send(record, (metadata, exception) -> {
            // Kafka 回调提供异常时执行统一分类。
            if (exception != null) {
                // 完成标准失败、拒绝或不确定结果。
                resultFuture.complete(classifySendFailure(exception));
                // 异常分支结束。
                return;
            }
            // 组合 Topic、Partition 和 Offset 作为便于诊断的原生位置标识。
            String nativeMessageId = metadata.topic()
                    + "-" + metadata.partition()
                    + "@" + metadata.offset();
            // Kafka Broker 已返回成功 metadata，映射为 CONFIRMED。
            resultFuture.complete(ProviderSendResult.confirmed(nativeMessageId));
        });
        // 返回异步标准结果。
        return resultFuture;
    }

    /**
     * 创建并启动一个专用 Kafka Consumer Worker。
     */
    @Override
    public MessageConsumer subscribe(ProviderSubscription subscription) {
        // Provider 关闭后不允许创建新消费者。
        if (closed.get()) {
            // 消费者属于启动资源，直接失败比返回无效句柄更安全。
            throw new IllegalStateException("Kafka provider is closed");
        }
        // 为当前消费者创建内部唯一标识。
        String workerId = UUID.randomUUID().toString();
        // 创建 Worker。
        KafkaConsumerWorker worker = new KafkaConsumerWorker(workerId, subscription);
        // 先登记 Worker，确保 Provider.close 可以发现它。
        workers.put(workerId, worker);
        // 启动专用 poll 线程。
        worker.start();
        // 返回 Worker 作为统一关闭句柄。
        return worker;
    }

    /**
     * 关闭 Producer 和全部 Consumer Worker。
     */
    @Override
    public void close() {
        // 只允许第一次调用执行关闭。
        if (closed.compareAndSet(false, true)) {
            // 通知全部 Worker 停止。
            workers.values().forEach(KafkaConsumerWorker::close);
            // 清空 Worker 注册表。
            workers.clear();
            // 给 Producer 一个有限关闭时间。
            producer.close(Duration.ofSeconds(5));
        }
    }

    /**
     * 将 Kafka 发送异常转换为 Provider 标准结果。
     */
    private static ProviderSendResult classifySendFailure(Exception exception) {
        // 超时发生时无法可靠证明 Broker 是否已经写入，因此返回 UNKNOWN。
        if (exception instanceof TimeoutException) {
            // 使用统一超时分类。
            return ProviderSendResult.of(
                    SendStatus.UNKNOWN,
                    SendFailureType.TIMEOUT,
                    exception.getMessage());
        }
        // 认证和授权失败属于明确拒绝。
        if (exception instanceof AuthenticationException
                || exception instanceof AuthorizationException) {
            // 不应对权限问题进行普通发送重试。
            return ProviderSendResult.of(
                    SendStatus.REJECTED,
                    SendFailureType.AUTHENTICATION_ERROR,
                    exception.getMessage());
        }
        // 其他 Kafka 回调异常按客户端或网络失败处理。
        return ProviderSendResult.of(
                SendStatus.FAILED,
                SendFailureType.CLIENT_ERROR,
                exception.getMessage());
    }

    /**
     * 将 Kafka Headers 转换为统一字符串消息头。
     */
    private static Map<String, String> toHeaders(ConsumerRecord<String, byte[]> record) {
        // 使用有序映射，重复 Header 保留最后一个值。
        Map<String, String> headers = new LinkedHashMap<>();
        // 遍历 Kafka 原生 Header。
        for (Header header : record.headers()) {
            // null Header 值按空字符串处理，避免构造 Map 时出现 null。
            String value = header.value() == null
                    ? ""
                    : new String(header.value(), StandardCharsets.UTF_8);
            // 写入标准映射。
            headers.put(header.key(), value);
        }
        // 返回由 ProviderInboundMessage 再次防御复制的映射。
        return headers;
    }

    /**
     * 每个业务订阅对应一个 KafkaConsumer 和一个专用 poll 线程。
     */
    private final class KafkaConsumerWorker implements MessageConsumer, Runnable {

        /** Worker 内部唯一标识。 */
        private final String workerId;

        /** Provider 订阅定义。 */
        private final ProviderSubscription subscription;

        /** KafkaConsumer 只能在当前 Worker 线程中使用。 */
        private final KafkaConsumer<String, byte[]> consumer;

        /** 专用 poll 线程。 */
        private final Thread thread;

        /** 标记 Worker 是否正在运行。 */
        private final AtomicBoolean running = new AtomicBoolean(true);

        /**
         * 创建 Kafka Consumer Worker。
         */
        private KafkaConsumerWorker(String workerId, ProviderSubscription subscription) {
            // 保存 Worker 标识。
            this.workerId = workerId;
            // 保存订阅定义。
            this.subscription = subscription;
            // 创建 Consumer 原生配置。
            Map<String, Object> properties = new LinkedHashMap<>();
            // 设置 Broker 地址。
            properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers());
            // 设置业务消费组。
            properties.put(ConsumerConfig.GROUP_ID_CONFIG, subscription.consumerGroup());
            // 每个 Worker 使用独立客户端标识。
            properties.put(
                    ConsumerConfig.CLIENT_ID_CONFIG,
                    config.clientId() + "-consumer-" + workerId);
            // 消息键反序列化为 String。
            properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            // 消息体保留 byte[] 交给 core 反序列化。
            properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
            // 禁止自动提交，确保业务 SUCCESS 后才推进 offset。
            properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
            // 没有历史 offset 时从最早位置开始，便于开发验证。
            properties.putIfAbsent(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            // 允许调用方覆盖或增加 Consumer 原生配置。
            properties.putAll(config.consumerProperties());
            // 强制禁止自动提交，避免扩展配置破坏组件语义。
            properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
            // 创建 KafkaConsumer。
            this.consumer = new KafkaConsumer<>(properties);
            // 创建专用守护线程。
            this.thread = new Thread(
                    this,
                    "iron-message-kafka-" + subscription.consumerGroup() + "-" + workerId);
            // 设置为守护线程。
            this.thread.setDaemon(true);
        }

        /**
         * 启动 Worker。
         */
        private void start() {
            // 启动专用 poll 线程。
            thread.start();
        }

        /**
         * 执行 Kafka poll 循环。
         */
        @Override
        public void run() {
            // KafkaConsumer 必须在使用线程中订阅 Topic。
            consumer.subscribe(List.of(subscription.destination().logicalName()));
            // 持续 poll 直到关闭。
            try {
                // 外层循环处理持续消费。
                while (running.get()) {
                    // 拉取一批 Kafka 记录。
                    ConsumerRecords<String, byte[]> records = consumer.poll(config.pollTimeout());
                    // 按 Kafka 返回顺序逐条处理。
                    for (ConsumerRecord<String, byte[]> record : records) {
                        // 关闭请求到达时尽快停止处理新记录。
                        if (!running.get()) {
                            // 跳出当前批次。
                            break;
                        }
                        // 将 Kafka 记录转换为统一入站消息。
                        ProviderInboundMessage inboundMessage = new ProviderInboundMessage(
                                subscription.destination(),
                                record.topic() + "-" + record.partition() + "@" + record.offset(),
                                record.key(),
                                toHeaders(record),
                                record.value(),
                                1);
                        // 默认按 RETRY 处理，避免监听器异常导致误提交。
                        ConsumeDecision decision = ConsumeDecision.RETRY;
                        // 调用 core 监听器。
                        try {
                            // 获取最终业务消费决策。
                            decision = subscription.listener().onMessage(inboundMessage);
                        } catch (RuntimeException ignored) {
                            // 异常保持 RETRY，二期再接入异常分类和死信。
                        }
                        // SUCCESS 时仅提交当前分区当前记录的下一个 offset。
                        if (decision == ConsumeDecision.SUCCESS) {
                            // 构造当前分区标识。
                            TopicPartition partition = new TopicPartition(
                                    record.topic(),
                                    record.partition());
                            // offset 提交语义是“下一条待消费记录”。
                            OffsetAndMetadata nextOffset = new OffsetAndMetadata(record.offset() + 1);
                            // 同步提交确保 ACK 结果明确后再继续。
                            consumer.commitSync(Map.of(partition, nextOffset));
                            // 当前记录处理完成，继续下一条。
                            continue;
                        }
                        // RETRY 时把当前分区位置退回当前记录。
                        TopicPartition partition = new TopicPartition(
                                record.topic(),
                                record.partition());
                        // 下一次 poll 将重新读取当前记录。
                        consumer.seek(partition, record.offset());
                        // 执行本地短暂退避，避免业务故障时高速空转。
                        sleep(config.retryBackoff());
                        // 当前批次后续记录不能越过失败记录继续提交。
                        break;
                    }
                }
            } catch (WakeupException exception) {
                // 只有运行状态仍为 true 时才代表非关闭导致的异常。
                if (running.get()) {
                    // 第一版直接结束 Worker；二期增加统一消费事件和重启治理。
                    throw exception;
                }
            } finally {
                // KafkaConsumer 必须由使用它的同一线程关闭。
                consumer.close(Duration.ofSeconds(5));
                // 从 Provider 注册表移除当前 Worker。
                workers.remove(workerId, this);
            }
        }

        /**
         * 停止 Worker。
         */
        @Override
        public void close() {
            // 只在第一次关闭时唤醒 poll。
            if (running.compareAndSet(true, false)) {
                // wakeup 是跨线程终止 KafkaConsumer.poll 的标准方式。
                consumer.wakeup();
            }
        }

        /**
         * 执行可中断退避。
         */
        private void sleep(Duration duration) {
            // 零退避无需休眠。
            if (duration.isZero()) {
                // 直接返回。
                return;
            }
            // 使用毫秒级休眠。
            try {
                // 阻塞当前 Consumer Worker。
                Thread.sleep(duration.toMillis());
            } catch (InterruptedException exception) {
                // 恢复中断标记。
                Thread.currentThread().interrupt();
                // 停止后续消费循环。
                running.set(false);
            }
        }
    }
}
