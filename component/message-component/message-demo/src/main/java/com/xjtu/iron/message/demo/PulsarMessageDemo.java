package com.xjtu.iron.message.demo;

import com.xjtu.iron.message.api.ConsumeDecision;
import com.xjtu.iron.message.api.ConsumerDefinition;
import com.xjtu.iron.message.api.MessageContext;
import com.xjtu.iron.message.api.MessageDestination;
import com.xjtu.iron.message.api.MessageEnvelope;
import com.xjtu.iron.message.api.MessageSubscription;
import com.xjtu.iron.message.api.SendResult;
import com.xjtu.iron.message.core.DestinationRoute;
import com.xjtu.iron.message.core.DestinationRouteRegistry;
import com.xjtu.iron.message.core.DestinationRoutingMode;
import com.xjtu.iron.message.core.MessageComponentOptions;
import com.xjtu.iron.message.core.MessageProviderRegistry;
import com.xjtu.iron.message.core.MessageTemplate;
import com.xjtu.iron.message.integration.pulsar.PulsarMessageProvider;
import com.xjtu.iron.message.integration.pulsar.PulsarMessageProviderConfig;
import com.xjtu.iron.message.testkit.Utf8StringMessageSerializer;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 使用真实 Pulsar 集群验证 message-component 普通消息发送、消费和 Negative ACK 重投。
 *
 * <p>默认连接当前 K8s 对外地址：</p>
 *
 * <pre>
 * pulsar://pulsar.xjtu-iron.online:6650
 * </pre>
 *
 * <p>所有关键值都可以通过环境变量覆盖，因此该 Demo 不把测试环境配置写死到 Provider 内部。</p>
 */
public final class PulsarMessageDemo {

    /** Pulsar 二进制服务地址环境变量。 */
    private static final String SERVICE_URL_ENV = "IRON_PULSAR_SERVICE_URL";

    /** Pulsar 完整 Topic 环境变量。 */
    private static final String TOPIC_ENV = "IRON_PULSAR_TOPIC";

    /** Pulsar Subscription Name 环境变量。 */
    private static final String SUBSCRIPTION_ENV = "IRON_PULSAR_SUBSCRIPTION";

    /** Pulsar Token 环境变量。 */
    private static final String TOKEN_ENV = "IRON_PULSAR_TOKEN";

    /** 是否在首次消费时主动返回 RETRY 的环境变量。 */
    private static final String RETRY_ONCE_ENV = "IRON_PULSAR_RETRY_ONCE";

    /** 等待消费完成秒数环境变量。 */
    private static final String WAIT_SECONDS_ENV = "IRON_PULSAR_WAIT_SECONDS";

    /** 当前 K8s Pulsar 对外二进制地址。 */
    private static final String DEFAULT_SERVICE_URL =
            "pulsar://pulsar.xjtu-iron.online:6650";

    /** 使用 public/default，避免调试阶段额外依赖 Tenant 和 Namespace 创建流程。 */
    private static final String DEFAULT_TOPIC =
            "persistent://public/default/iron-message-component-debug";

    /** 默认调试订阅名称。 */
    private static final String DEFAULT_SUBSCRIPTION =
            "iron-message-component-debug-subscription";

    /** 默认等待消费完成时间。 */
    private static final int DEFAULT_WAIT_SECONDS = 30;

    /** 工具类不允许实例化。 */
    private PulsarMessageDemo() {
    }

    /**
     * 启动真实 Pulsar 基础闭环验证。
     *
     * @param args 当前 Demo 不使用命令行参数
     * @throws Exception 连接、发送、等待或关闭失败时向上抛出
     */
    public static void main(String[] args) throws Exception {
        // 读取并打印本次调试配置，Token 只打印是否存在，绝不输出原值。
        DebugSettings settings = DebugSettings.fromEnvironment();
        // 输出开始信息，便于和 Pulsar Broker 日志对照。
        printSettings(settings);

        // 创建适合公网链路的 Pulsar Client 配置。
        PulsarMessageProviderConfig providerConfig =
                PulsarMessageProviderConfig.externalDebug(
                        settings.serviceUrl,
                        settings.authenticationToken);
        // 创建真实 Pulsar Provider。
        PulsarMessageProvider provider = new PulsarMessageProvider(providerConfig);
        // 将 Pulsar Provider 注册为当前唯一 Provider。
        MessageProviderRegistry providers =
                new MessageProviderRegistry(List.of(provider));

        // 定义业务侧逻辑目的地；物理 Topic 仍由精确路由决定。
        MessageDestination destination =
                MessageDestination.of("message-debug", "pulsar-basic");
        // 创建严格路由，禁止逻辑名称拼错后隐式发送到其他 Topic。
        DestinationRouteRegistry routes = new DestinationRouteRegistry(List.of(
                DestinationRoute.of(
                        destination,
                        PulsarMessageProvider.NAME,
                        settings.topic)));
        // 公网调试将 core 确认等待时间提高到十五秒，与 Provider 操作超时保持一致。
        MessageComponentOptions options = new MessageComponentOptions(
                PulsarMessageProvider.NAME,
                "iron-message-pulsar-debug",
                "1",
                Duration.ofSeconds(15),
                DestinationRoutingMode.STRICT,
                Clock.systemUTC());
        // 使用 UTF-8 String 序列化器完成最小变量验证，暂时不引入业务 JSON 模型。
        MessageTemplate template = MessageTemplate.create(
                options,
                providers,
                routes,
                new Utf8StringMessageSerializer());

        // 等待最终成功消费。
        CountDownLatch consumed = new CountDownLatch(1);
        // 记录当前 JVM 中该消息的处理次数，用于可选的首次 RETRY 验证。
        AtomicInteger localDeliveries = new AtomicInteger();
        // Subscription 句柄用于 finally 中精确关闭当前消费者。
        MessageSubscription subscription = null;

        try {
            // 先创建同步订阅，再发送消息，避免调试消息在消费者就绪前被遗漏。
            subscription = template.subscribe(
                    ConsumerDefinition.of(
                            destination,
                            settings.subscriptionName,
                            String.class),
                    (message, context) -> {
                        // 记录当前 JVM 中收到该消息的次数。
                        int delivery = localDeliveries.incrementAndGet();
                        // 输出统一消息模型和 Pulsar 原生诊断元数据。
                        System.out.println("\n========== PULSAR CONSUME ==========");
                        System.out.println("localDelivery=" + delivery);
                        System.out.println("messageId=" + message.messageId());
                        System.out.println("messageKey=" + message.messageKey());
                        System.out.println("messageType=" + message.messageType());
                        System.out.println("payload=" + message.payload());
                        System.out.println("correlationId=" + message.context().correlationId());
                        System.out.println("causationId=" + message.context().causationId());
                        System.out.println("provider=" + context.providerName());
                        System.out.println("physicalDestination=" + context.physicalDestination());
                        System.out.println("consumerGroup=" + context.consumerGroup());
                        System.out.println("providerMessageId=" + context.providerMessageId());
                        System.out.println("providerMetadata=" + context.metadata());

                        // 开启 RETRY_ONCE 时，首次处理主动 Negative ACK，验证 Pulsar 重新投递。
                        if (settings.retryOnce && delivery == 1) {
                            // 明确打印本次决策，便于观察 redelivery-count 是否增加。
                            System.out.println("consumeDecision=RETRY (debug first delivery)");
                            // 返回 RETRY，由 Pulsar Provider 执行 Negative ACK。
                            return ConsumeDecision.RETRY;
                        }

                        // 最终成功处理后释放等待线程。
                        consumed.countDown();
                        // 返回 SUCCESS，由 Pulsar Provider 执行异步 ACK。
                        System.out.println("consumeDecision=SUCCESS");
                        return ConsumeDecision.SUCCESS;
                    });

            // 为每次调试生成独立运行标识。
            String runId = UUID.randomUUID().toString();
            // 构造一条包含 key、上下文和用户 Header 的完整统一消息。
            MessageEnvelope<String> message = MessageEnvelope.builder(
                            "PulsarDebugMessage",
                            "runId=" + runId + ", sentAt=" + Instant.now())
                    // messageKey 用于业务实体关联；当前 Shared Subscription 不承诺相同 Key 顺序。
                    .messageKey("pulsar-debug-key")
                    // 显式设置 correlationId，便于消费端核对上下文是否完整还原。
                    .context(MessageContext.builder()
                            .correlationId("pulsar-debug-flow-" + runId)
                            .build())
                    // 添加普通业务 Header，验证 Pulsar Properties 往返映射。
                    .header("debug-run-id", runId)
                    .build();

            // 通过统一 MessageTemplate 同步发送并等待标准结果。
            SendResult result = template.send(destination, message);
            // 输出完整发送结果。
            printSendResult(result);

            // 没有获得明确确认时直接停止，避免把 UNKNOWN 或 FAILED 当作成功继续等待。
            if (!result.confirmed()) {
                // 抛出包含阶段和失败类型的错误，便于快速定位连接、路由或认证问题。
                throw new IllegalStateException(
                        "Pulsar send was not confirmed: status=" + result.status()
                                + ", stage=" + result.stage()
                                + ", failureType=" + result.failureType()
                                + ", description=" + result.description());
            }

            // 等待 Consumer 最终返回 SUCCESS。
            if (!consumed.await(settings.waitSeconds, TimeUnit.SECONDS)) {
                // 发送成功但未消费时，重点检查 Subscription、Topic、Proxy 路由和 Broker 广播地址。
                throw new IllegalStateException(
                        "Pulsar consumer timed out after " + settings.waitSeconds
                                + " seconds; send was confirmed but no final SUCCESS was observed");
            }

            // 输出闭环成功标识。
            System.out.println("\nPULSAR MESSAGE COMPONENT DEBUG SUCCESS");
        } finally {
            // 优先关闭当前订阅。
            if (subscription != null) {
                // 停止 Consumer 并释放订阅端客户端资源。
                subscription.close();
            }
            // 关闭 MessageTemplate，同时关闭 Provider、Producer 和 PulsarClient。
            template.close();
        }
    }

    /** 输出本次连接参数。 */
    private static void printSettings(DebugSettings settings) {
        // 输出服务地址。
        System.out.println("========== PULSAR DEBUG SETTINGS ==========");
        System.out.println("serviceUrl=" + settings.serviceUrl);
        // 输出完整 Topic。
        System.out.println("topic=" + settings.topic);
        // 输出 Subscription Name。
        System.out.println("subscription=" + settings.subscriptionName);
        // 只输出是否配置 Token。
        System.out.println("authenticationConfigured="
                + (settings.authenticationToken != null));
        // 输出是否验证一次 Negative ACK。
        System.out.println("retryOnce=" + settings.retryOnce);
        // 输出最长等待秒数。
        System.out.println("waitSeconds=" + settings.waitSeconds);
    }

    /** 输出统一发送结果。 */
    private static void printSendResult(SendResult result) {
        // 分隔发送结果。
        System.out.println("\n========== PULSAR SEND ==========");
        // 输出组件消息 ID。
        System.out.println("messageId=" + result.messageId());
        // 输出实际 Provider。
        System.out.println("provider=" + result.providerName());
        // 输出物理 Topic。
        System.out.println("physicalDestination=" + result.physicalDestination());
        // 输出标准状态。
        System.out.println("status=" + result.status());
        // 输出失败阶段。
        System.out.println("stage=" + result.stage());
        // 输出失败类型。
        System.out.println("failureType=" + result.failureType());
        // 输出 Pulsar 原生消息 ID。
        System.out.println("providerMessageId=" + result.providerMessageId());
        // 输出诊断描述。
        System.out.println("description=" + result.description());
        // 输出 Provider 诊断元数据。
        System.out.println("metadata=" + result.metadata());
    }

    /** 表示仅供当前调试入口使用的环境配置快照。 */
    private static final class DebugSettings {

        /** Pulsar 二进制服务地址。 */
        private final String serviceUrl;

        /** 完整物理 Topic。 */
        private final String topic;

        /** Subscription Name。 */
        private final String subscriptionName;

        /** 可选 Token。 */
        private final String authenticationToken;

        /** 是否首次主动返回 RETRY。 */
        private final boolean retryOnce;

        /** 最长等待消费完成秒数。 */
        private final int waitSeconds;

        /** 创建调试配置快照。 */
        private DebugSettings(
                String serviceUrl,
                String topic,
                String subscriptionName,
                String authenticationToken,
                boolean retryOnce,
                int waitSeconds) {
            // 保存服务地址。
            this.serviceUrl = serviceUrl;
            // 保存完整 Topic。
            this.topic = topic;
            // 保存订阅名称。
            this.subscriptionName = subscriptionName;
            // 保存可选 Token。
            this.authenticationToken = authenticationToken;
            // 保存是否首次重试。
            this.retryOnce = retryOnce;
            // 保存等待时间。
            this.waitSeconds = waitSeconds;
        }

        /** 从环境变量创建配置快照。 */
        private static DebugSettings fromEnvironment() {
            // 读取服务地址，缺失时使用当前 K8s 公网地址。
            String serviceUrl = environmentOrDefault(
                    SERVICE_URL_ENV,
                    DEFAULT_SERVICE_URL);
            // 读取完整 Topic。
            String topic = environmentOrDefault(
                    TOPIC_ENV,
                    DEFAULT_TOPIC);
            // 读取订阅名称。
            String subscriptionName = environmentOrDefault(
                    SUBSCRIPTION_ENV,
                    DEFAULT_SUBSCRIPTION);
            // Token 为空时保持 null。
            String token = normalize(System.getenv(TOKEN_ENV));
            // 读取是否验证一次 Negative ACK。
            boolean retryOnce = Boolean.parseBoolean(
                    environmentOrDefault(RETRY_ONCE_ENV, "false")
                            .toLowerCase(Locale.ROOT));
            // 读取等待秒数并校验为正整数。
            int waitSeconds = parsePositiveInt(
                    WAIT_SECONDS_ENV,
                    environmentOrDefault(
                            WAIT_SECONDS_ENV,
                            Integer.toString(DEFAULT_WAIT_SECONDS)));
            // 返回不可变调试配置。
            return new DebugSettings(
                    serviceUrl,
                    topic,
                    subscriptionName,
                    token,
                    retryOnce,
                    waitSeconds);
        }
    }

    /** 读取环境变量；空白时返回默认值。 */
    private static String environmentOrDefault(
            String name,
            String defaultValue) {
        // 读取环境变量并标准化。
        String value = normalize(System.getenv(name));
        // 缺失时使用默认值。
        return value == null ? defaultValue : value;
    }

    /** 标准化可选文本。 */
    private static String normalize(String value) {
        // null 保持 null。
        if (value == null) {
            // 返回 null 表示未配置。
            return null;
        }
        // 去除首尾空白。
        String trimmed = value.trim();
        // 空白字符串转换为 null。
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 解析正整数环境变量。 */
    private static int parsePositiveInt(
            String name,
            String value) {
        // 捕获非法数字格式。
        try {
            // 解析整数。
            int parsed = Integer.parseInt(value);
            // 非正数不允许作为等待时间。
            if (parsed <= 0) {
                // 抛出明确配置错误。
                throw new IllegalArgumentException(name + " must be positive");
            }
            // 返回合法整数。
            return parsed;
        } catch (NumberFormatException exception) {
            // 将格式错误转换为明确配置异常。
            throw new IllegalArgumentException(
                    name + " must be a positive integer: " + value,
                    exception);
        }
    }
}
