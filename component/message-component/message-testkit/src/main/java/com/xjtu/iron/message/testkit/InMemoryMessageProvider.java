package com.xjtu.iron.message.testkit;

import com.xjtu.iron.message.api.ConsumeDecision;
import com.xjtu.iron.message.api.MessageConsumer;
import com.xjtu.iron.message.api.SendFailureType;
import com.xjtu.iron.message.api.SendStatus;
import com.xjtu.iron.message.api.spi.*;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 为单元测试和本地示例提供的内存消息 Provider。
 *
 * <p>它用于验证 message-core 生命周期，不模拟任何特定 MQ 的完整协议。</p>
 */
public final class InMemoryMessageProvider implements MessageProvider {

    /** Provider 对外稳定名称。 */
    public static final String NAME = "memory";

    /** 按逻辑目的地保存已注册订阅。 */
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Registration>> registrations =
            new ConcurrentHashMap<>();

    /** 执行异步投递和重投调度的单线程执行器。 */
    private final ScheduledExecutorService executor;

    /** 失败后的最大额外投递次数。 */
    private final int maxRedeliveries;

    /** 两次投递之间的等待时间。 */
    private final Duration redeliveryDelay;

    /** 标记 Provider 是否已经关闭。 */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * 创建使用默认重投参数的内存 Provider。
     */
    public InMemoryMessageProvider() {
        // 默认额外重投两次，每次间隔十毫秒。
        this(2, Duration.ofMillis(10));
    }

    /**
     * 创建可控制重投参数的内存 Provider。
     *
     * @param maxRedeliveries 最大额外投递次数
     * @param redeliveryDelay 重投等待时间
     */
    public InMemoryMessageProvider(int maxRedeliveries, Duration redeliveryDelay) {
        // 最大重投次数不能为负数。
        if (maxRedeliveries < 0) {
            // 负数没有明确含义。
            throw new IllegalArgumentException("maxRedeliveries must not be negative");
        }
        // 重投间隔不能为空且不能为负数。
        if (redeliveryDelay == null || redeliveryDelay.isNegative()) {
            // 无效等待时间应在启动阶段失败。
            throw new IllegalArgumentException("redeliveryDelay must not be null or negative");
        }
        // 保存最大重投次数。
        this.maxRedeliveries = maxRedeliveries;
        // 保存重投等待时间。
        this.redeliveryDelay = redeliveryDelay;
        // 创建守护线程，避免示例退出时被测试线程阻塞。
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            // 创建具有可识别名称的消费线程。
            Thread thread = new Thread(runnable, "iron-message-memory-provider");
            // 设置为守护线程。
            thread.setDaemon(true);
            // 返回配置完成的线程。
            return thread;
        });
    }

    /**
     * 返回内存 Provider 名称。
     */
    @Override
    public String name() {
        // 返回稳定名称。
        return NAME;
    }

    /**
     * 返回内存 Provider 支持的基础能力。
     */
    @Override
    public Set<MessageCapability> capabilities() {
        // 内存实现同时支持基础发布和基础消费。
        return Set.of(
                MessageCapability.BASIC_PUBLISH,
                MessageCapability.BASIC_CONSUME);
    }

    /**
     * 异步确认发送并调度内存投递。
     */
    @Override
    public CompletionStage<ProviderSendResult> send(ProviderSendRequest request) {
        // Provider 已关闭时返回明确失败结果。
        if (closed.get()) {
            // 不抛异常，使业务仍获得统一发送结果。
            return CompletableFuture.completedFuture(ProviderSendResult.of(
                    SendStatus.FAILED,
                    SendFailureType.CLIENT_ERROR,
                    "in-memory provider is closed"));
        }
        // 创建模拟中间件原生消息标识。
        String nativeMessageId = UUID.randomUUID().toString();
        // 查找当前目的地下全部订阅。
        List<Registration> destinationRegistrations = registrations.getOrDefault(
                request.destination().logicalName(),
                new CopyOnWriteArrayList<>());
        // 为每个订阅独立调度首次投递。
        for (Registration registration : destinationRegistrations) {
            // 首次投递 attempt 从 1 开始。
            scheduleDelivery(registration, request, nativeMessageId, 1, Duration.ZERO);
        }
        // 内存接收完成后立即返回明确确认。
        return CompletableFuture.completedFuture(
                ProviderSendResult.confirmed(nativeMessageId));
    }

    /**
     * 注册一个内存消费者。
     */
    @Override
    public MessageConsumer subscribe(ProviderSubscription subscription) {
        // Provider 已关闭后不允许新增消费者。
        if (closed.get()) {
            // 启动期配置错误使用异常直接暴露。
            throw new IllegalStateException("in-memory provider is closed");
        }
        // 创建订阅注册对象。
        Registration registration = new Registration(subscription);
        // 按逻辑目的地保存订阅。
        registrations.computeIfAbsent(
                subscription.destination().logicalName(),
                ignored -> new CopyOnWriteArrayList<>())
                .add(registration);
        // 返回注册对象本身作为消费者关闭句柄。
        return registration;
    }

    /**
     * 关闭 Provider。
     */
    @Override
    public void close() {
        // 仅第一次关闭执行资源释放。
        if (closed.compareAndSet(false, true)) {
            // 停止接收新的调度任务。
            executor.shutdownNow();
            // 清空全部订阅。
            registrations.clear();
        }
    }

    /**
     * 调度一次消息投递。
     */
    private void scheduleDelivery(
            Registration registration,
            ProviderSendRequest request,
            String nativeMessageId,
            int attempt,
            Duration delay) {
        // 关闭后的注册不再投递。
        if (registration.closed.get() || closed.get()) {
            // 直接结束当前投递计划。
            return;
        }
        // 将投递任务放入调度线程。
        executor.schedule(
                () -> deliver(registration, request, nativeMessageId, attempt),
                delay.toMillis(),
                TimeUnit.MILLISECONDS);
    }

    /**
     * 执行一次实际内存投递。
     */
    private void deliver(
            Registration registration,
            ProviderSendRequest request,
            String nativeMessageId,
            int attempt) {
        // 消费者或 Provider 已关闭时不再调用业务。
        if (registration.closed.get() || closed.get()) {
            // 结束当前任务。
            return;
        }
        // 创建 Provider 入站消息。
        ProviderInboundMessage inboundMessage = new ProviderInboundMessage(
                request.destination(),
                nativeMessageId,
                request.key(),
                request.headers(),
                request.payload(),
                attempt);
        // 默认采用 RETRY，确保监听器异常不会被误判成功。
        ConsumeDecision decision = ConsumeDecision.RETRY;
        // 调用 core 监听器。
        try {
            // 获取业务最终消费决策。
            decision = registration.subscription.listener().onMessage(inboundMessage);
        } catch (RuntimeException ignored) {
            // 内存 Provider 不吞并为成功，仍保持 RETRY。
        }
        // 只有 RETRY 且未超过最大次数时才继续调度。
        if (decision == ConsumeDecision.RETRY && attempt <= maxRedeliveries) {
            // 下一次投递次数递增。
            int nextAttempt = attempt + 1;
            // 使用统一重投间隔再次调度。
            scheduleDelivery(
                    registration,
                    request,
                    nativeMessageId,
                    nextAttempt,
                    redeliveryDelay);
        }
    }

    /**
     * 保存一个内存订阅及其关闭状态。
     */
    private final class Registration implements MessageConsumer {

        /** 原始 Provider 订阅定义。 */
        private final ProviderSubscription subscription;

        /** 标记当前订阅是否已关闭。 */
        private final AtomicBoolean closed = new AtomicBoolean(false);

        /**
         * 创建订阅注册对象。
         *
         * @param subscription Provider 订阅定义
         */
        private Registration(ProviderSubscription subscription) {
            // 保存订阅定义。
            this.subscription = subscription;
        }

        /**
         * 关闭当前订阅。
         */
        @Override
        public void close() {
            // 仅第一次关闭时执行移除。
            if (closed.compareAndSet(false, true)) {
                // 获取当前目的地对应的注册列表。
                CopyOnWriteArrayList<Registration> destinationRegistrations =
                        registrations.get(subscription.destination().logicalName());
                // 目的地列表可能已经在 Provider 关闭时被清理。
                if (destinationRegistrations != null) {
                    // 从列表中移除当前订阅。
                    destinationRegistrations.remove(this);
                    // 空列表不再保留，避免长期测试产生无效键。
                    if (destinationRegistrations.isEmpty()) {
                        // 仅在映射仍指向同一列表时删除。
                        registrations.remove(
                                subscription.destination().logicalName(),
                                destinationRegistrations);
                    }
                }
            }
        }
    }
}
