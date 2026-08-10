package com.xjtu.iron.message.core.send;


import com.xjtu.iron.message.api.MessageDestination;
import com.xjtu.iron.message.api.MessageEnvelope;
import com.xjtu.iron.message.spi.MessageProvider;
import com.xjtu.iron.message.spi.ProviderDestination;
import com.xjtu.iron.message.spi.ProviderSendRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 表示已经完成发送准备的不可变快照。
 *
 * <p>
 * MessageTemplate 只负责构建这个快照，后续直发或可靠发送都基于它执行。
 * </p>
 */
public final class PreparedMessageSend {

    /** 业务调用时指定的逻辑目的地。 */
    private final MessageDestination destination;

    /** 已经经过 enrich 的消息信封。 */
    private final MessageEnvelope<?> message;

    /** 已解析出的 Provider 物理目的地。 */
    private final ProviderDestination providerDestination;

    /** 实际执行发送的 Provider。 */
    private final MessageProvider provider;

    /** 交给 Provider 的线级发送请求。 */
    private final ProviderSendRequest request;

    /** 单次发送等待确认的最大时间。 */
    private final Duration confirmTimeout;

    /** 整个发送调用开始时间。 */
    private final Instant startedAt;

    public PreparedMessageSend(
            MessageDestination destination,
            MessageEnvelope<?> message,
            ProviderDestination providerDestination,
            MessageProvider provider,
            ProviderSendRequest request,
            Duration confirmTimeout,
            Instant startedAt) {
        this.destination = Objects.requireNonNull(destination, "destination must not be null");
        this.message = Objects.requireNonNull(message, "message must not be null");
        this.providerDestination = Objects.requireNonNull(providerDestination, "providerDestination must not be null");
        this.provider = Objects.requireNonNull(provider, "provider must not be null");
        this.request = Objects.requireNonNull(request, "request must not be null");
        this.confirmTimeout = Objects.requireNonNull(confirmTimeout, "confirmTimeout must not be null");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt must not be null");
    }

    public MessageDestination destination() {
        return destination;
    }

    public MessageEnvelope<?> message() {
        return message;
    }

    public ProviderDestination providerDestination() {
        return providerDestination;
    }

    public MessageProvider provider() {
        return provider;
    }

    public ProviderSendRequest request() {
        return request;
    }

    public Duration confirmTimeout() {
        return confirmTimeout;
    }

    public Instant startedAt() {
        return startedAt;
    }
}
