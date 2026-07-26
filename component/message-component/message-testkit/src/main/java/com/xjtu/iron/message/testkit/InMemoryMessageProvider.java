package com.xjtu.iron.message.testkit;

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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用于单元测试和 Demo 的内存普通消息 Provider。
 *
 * <p>该实现只模拟一期公共语义，不模拟真实 Broker 的持久化、副本、重平衡和故障窗口。</p>
 */
public final class InMemoryMessageProvider implements MessageProvider {

    /** 默认 Provider 名称。 */
    public static final String NAME = "memory";

    /** Provider 名称。 */
    private final String name;

    /** 最大本地投递次数。 */
    private final int maxDeliveryAttempts;

    /** RETRY 决策后的固定退避。 */
    private final Duration retryBackoff;

    /** 按物理目的地和消费组保存订阅。 */
    private final ConcurrentMap<String, ConcurrentMap<String, GroupSubscriptions>> subscriptions =
            new ConcurrentHashMap<>();

    /** 保存全部已确认发送记录。 */
    private final CopyOnWriteArrayList<InMemoryMessageRecord> records =
            new CopyOnWriteArrayList<>();

    /** 异步投递线程池。 */
    private final ExecutorService deliveryExecutor;

    /** Provider 关闭状态。 */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * 创建默认内存 Provider。
     */
    public InMemoryMessageProvider() {
        // 默认最多投递三次，每次间隔十毫秒。
        this(NAME, 3, Duration.ofMillis(10));
    }

    /**
     * 创建可配置内存 Provider。
     *
     * @param name Provider 名称
     * @param maxDeliveryAttempts 最大投递次数
     * @param retryBackoff 重试退避
     */
    public InMemoryMessageProvider(
            String name,
            int maxDeliveryAttempts,
            Duration retryBackoff) {
        // Provider 名称必须存在。
        if (name == null || name.isBlank()) {
            // 拒绝非法名称。
            throw new IllegalArgumentException("name must not be blank");
        }
        // 保存标准化名称。
        this.name = name.trim();
        // 最大投递次数至少为 1。
        if (maxDeliveryAttempts < 1) {
            // 拒绝无投递配置。
            throw new IllegalArgumentException("maxDeliveryAttempts must be at least 1");
        }
        // 保存最大投递次数。
        this.maxDeliveryAttempts = maxDeliveryAttempts;
        // 退避不能为空或负数。
        if (retryBackoff == null || retryBackoff.isNegative()) {
            // 拒绝非法退避。
            throw new IllegalArgumentException("retryBackoff must not be negative");
        }
        // 保存退避。
        this.retryBackoff = retryBackoff;
        // 创建命名守护线程工厂。
        ThreadFactory threadFactory = runnable -> {
            // 创建投递线程。
            Thread thread = new Thread(runnable, "iron-message-memory-delivery");
            // 设置守护线程。
            thread.setDaemon(true);
            // 返回线程。
            return thread;
        };
        // CachedThreadPool 便于测试多个独立消费组并发投递。
        this.deliveryExecutor = Executors.newCachedThreadPool(threadFactory);
    }

    /**
     * 返回 Provider 名称。
     */
    @Override
    public String name() {
        // 返回构造时名称。
        return name;
    }

    /**
     * 返回一期公共能力。
     */
    @Override
    public Set<MessageCapability> capabilities() {
        // 支持普通发布和普通消费。
        return Set.of(
                MessageCapability.BASIC_PUBLISH,
                MessageCapability.BASIC_CONSUME);
    }

    /**
     * 保存并异步投递消息。
     */
    @Override
    public CompletionStage<ProviderSendResult> send(ProviderSendRequest request) {
        // Provider 关闭后返回明确本地失败。
        if (closed.get()) {
            // 不再接收新消息。
            return CompletableFuture.completedFuture(ProviderSendResult.failed(
                    SendStatus.FAILED,
                    SendFailureType.CLIENT_ERROR,
                    "in-memory provider is closed"));
        }
        // 生成内存 Provider 消息 ID。
        String providerMessageId = UUID.randomUUID().toString();
        // 创建线级历史记录。
        InMemoryMessageRecord record = new InMemoryMessageRecord(
                providerMessageId,
                request.destination().physicalName(),
                request.messageId(),
                request.key(),
                request.headers(),
                request.body(),
                Instant.now());
        // 保存历史记录。
        records.add(record);
        // 在确认后异步投递给每个消费组中的一个订阅实例。
        deliveryExecutor.execute(() -> dispatch(record));
        // 内存保存完成后立即返回明确确认。
        return CompletableFuture.completedFuture(ProviderSendResult.confirmed(
                providerMessageId,
                Map.of("physicalDestination", request.destination().physicalName())));
    }

    /**
     * 注册内存订阅。
     */
    @Override
    public ProviderSubscription subscribe(ProviderSubscriptionRequest request) {
        // Provider 关闭后拒绝新订阅。
        if (closed.get()) {
            // 启动阶段直接失败。
            throw new IllegalStateException("in-memory provider is closed");
        }
        // 创建订阅 ID。
        String subscriptionId = UUID.randomUUID().toString();
        // 获取物理目的地订阅表。
        ConcurrentMap<String, GroupSubscriptions> destinationSubscriptions =
                subscriptions.computeIfAbsent(
                        request.destination().physicalName(),
                        ignored -> new ConcurrentHashMap<>());
        // 获取消费组订阅集合。
        GroupSubscriptions groupSubscriptions = destinationSubscriptions.computeIfAbsent(
                request.consumerGroup(),
                ignored -> new GroupSubscriptions());
        // 创建订阅状态。
        SubscriptionState state = new SubscriptionState(subscriptionId, request);
        // 加入消费组。
        groupSubscriptions.states().add(state);
        // 返回关闭句柄。
        return () -> removeSubscription(
                request.destination().physicalName(),
                request.consumerGroup(),
                state);
    }

    /**
     * 返回全部已发送记录快照。
     *
     * @return 记录快照
     */
    public List<InMemoryMessageRecord> records() {
        // 创建独立列表，防止测试修改内部集合。
        return List.copyOf(records);
    }

    /**
     * 清空已发送记录。
     */
    public void clearRecords() {
        // 清空线程安全记录列表。
        records.clear();
    }

    /**
     * 关闭内存 Provider。
     */
    @Override
    public void close() {
        // 只允许第一次关闭执行清理。
        if (closed.compareAndSet(false, true)) {
            // 清空订阅。
            subscriptions.clear();
            // 停止异步投递线程池。
            deliveryExecutor.shutdownNow();
        }
    }

    /**
     * 将一条消息投递给全部消费组。
     */
    private void dispatch(InMemoryMessageRecord record) {
        // 获取物理目的地全部消费组。
        ConcurrentMap<String, GroupSubscriptions> destinationSubscriptions =
                subscriptions.get(record.physicalDestination());
        // 没有订阅时消息只保留历史记录。
        if (destinationSubscriptions == null || destinationSubscriptions.isEmpty()) {
            // 直接结束投递。
            return;
        }
        // 每个消费组独立获得一份消息。
        destinationSubscriptions.values().forEach(group -> {
            // 从组内订阅中轮询一个实例。
            SubscriptionState target = group.next();
            // 组内暂时没有有效订阅时跳过。
            if (target == null) {
                // 结束当前组投递。
                return;
            }
            // 对当前组执行带有限次数的投递。
            deliverToSubscription(record, target);
        });
    }

    /**
     * 投递给单个订阅并处理 RETRY。
     */
    private void deliverToSubscription(
            InMemoryMessageRecord record,
            SubscriptionState subscription) {
        // 从第一次投递开始循环。
        for (int attempt = 1; attempt <= maxDeliveryAttempts; attempt++) {
            // 订阅已经关闭时停止。
            if (!subscription.active().get() || closed.get()) {
                // 结束投递。
                return;
            }
            // 构造 Provider 入站消息。
            ProviderInboundMessage inbound = new ProviderInboundMessage(
                    record.providerMessageId(),
                    record.key(),
                    record.headers(),
                    record.body(),
                    attempt,
                    Instant.now(),
                    Map.of("provider", name));
            // 默认 RETRY。
            ConsumeDecision decision = ConsumeDecision.RETRY;
            // 调用 core 监听器。
            try {
                // 获取业务决策。
                decision = subscription.request().listener().onMessage(inbound);
            } catch (RuntimeException ignored) {
                // 异常保持 RETRY。
            }
            // SUCCESS 时完成当前消费组投递。
            if (decision == ConsumeDecision.SUCCESS) {
                // 立即返回。
                return;
            }
            // 最后一次失败后不再本地重试。
            if (attempt == maxDeliveryAttempts) {
                // 一期没有死信能力，结束投递。
                return;
            }
            // 执行固定退避。
            sleep(retryBackoff);
        }
    }

    /**
     * 移除订阅。
     */
    private void removeSubscription(
            String physicalDestination,
            String consumerGroup,
            SubscriptionState state) {
        // 只允许第一次关闭生效。
        if (!state.active().compareAndSet(true, false)) {
            // 已关闭时直接返回。
            return;
        }
        // 获取目的地订阅表。
        ConcurrentMap<String, GroupSubscriptions> destinationSubscriptions =
                subscriptions.get(physicalDestination);
        // 目的地已经被清理时直接返回。
        if (destinationSubscriptions == null) {
            // 无需继续。
            return;
        }
        // 获取消费组。
        GroupSubscriptions group = destinationSubscriptions.get(consumerGroup);
        // 消费组存在时移除状态。
        if (group != null) {
            // 删除当前订阅。
            group.states().remove(state);
            // 空组从目的地表移除。
            if (group.states().isEmpty()) {
                // 条件删除防止并发新增被误删。
                destinationSubscriptions.remove(consumerGroup, group);
            }
        }
        // 空目的地表从顶层移除。
        if (destinationSubscriptions.isEmpty()) {
            // 条件删除防止并发新增被误删。
            subscriptions.remove(physicalDestination, destinationSubscriptions);
        }
    }

    /**
     * 执行固定退避。
     */
    private static void sleep(Duration duration) {
        // 零退避直接返回。
        if (duration.isZero()) {
            // 不休眠。
            return;
        }
        // 捕获中断。
        try {
            // 使用毫秒级休眠。
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException exception) {
            // 恢复中断标记。
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 表示消费组内的订阅实例集合和轮询游标。
     */
    private static final class GroupSubscriptions {

        /** 组内订阅实例。 */
        private final CopyOnWriteArrayList<SubscriptionState> states =
                new CopyOnWriteArrayList<>();

        /** 轮询游标。 */
        private final AtomicInteger cursor = new AtomicInteger();

        /** @return 组内订阅列表 */
        private CopyOnWriteArrayList<SubscriptionState> states() {
            // 返回线程安全列表。
            return states;
        }

        /**
         * 轮询一个有效订阅。
         *
         * @return 有效订阅；不存在时为空
         */
        private SubscriptionState next() {
            // 获取当前快照大小。
            int size = states.size();
            // 空组返回 null。
            if (size == 0) {
                // 表示没有可投递实例。
                return null;
            }
            // 最多检查当前大小次数，跳过已关闭实例。
            for (int index = 0; index < size; index++) {
                // 使用 floorMod 防止游标溢出后出现负数。
                int selectedIndex = Math.floorMod(cursor.getAndIncrement(), size);
                // 获取候选实例。
                SubscriptionState candidate = states.get(selectedIndex);
                // 有效实例直接返回。
                if (candidate.active().get()) {
                    // 返回候选实例。
                    return candidate;
                }
            }
            // 没有有效实例。
            return null;
        }
    }

    /**
     * 表示一个内存订阅状态。
     *
     * @param id 订阅 ID
     * @param request Provider 订阅请求
     * @param active 活跃状态
     */
    private record SubscriptionState(
            String id,
            ProviderSubscriptionRequest request,
            AtomicBoolean active) {

        /**
         * 创建活跃订阅状态。
         */
        private SubscriptionState(
                String id,
                ProviderSubscriptionRequest request) {
            // 默认 active 为 true。
            this(id, request, new AtomicBoolean(true));
        }
    }
}
