package com.xjtu.iron.message.core.consume;

import com.xjtu.iron.message.api.consume.decision.ConsumeDecision;
import com.xjtu.iron.message.api.consume.definition.ConsumerDefinition;
import com.xjtu.iron.message.api.consume.handler.MessageHandler;
import com.xjtu.iron.message.api.consume.decision.ConsumeFailureType;
import com.xjtu.iron.message.core.codec.MessageWireCodec;
import com.xjtu.iron.message.spi.ProviderConsumeResult;
import com.xjtu.iron.message.spi.ProviderDestination;
import com.xjtu.iron.message.spi.ProviderInboundMessage;

import java.util.Objects;

/**
 * MQ 入站适配层。
 *
 * <p>负责 ProviderInboundMessage 到统一消费模型的转换，
 * 不负责幂等、事务和业务执行。</p>
 */
public final class MessageConsumerAdapter {

    private final MessageWireCodec wireCodec;
    private final MessageConsumeExecutor executor;

    public MessageConsumerAdapter(
            MessageWireCodec wireCodec,
            MessageConsumeExecutor executor) {
        this.wireCodec = Objects.requireNonNull(wireCodec);
        this.executor = Objects.requireNonNull(executor);
    }

    public <T> ProviderConsumeResult consume(
            ConsumerDefinition<T> definition,
            MessageHandler<T> handler,
            ProviderDestination destination,
            ProviderInboundMessage inbound) {
        try {
            MessageWireCodec.DecodedInbound<T> decoded = wireCodec.decode(
                    definition,
                    destination,
                    inbound);
            ConsumeDecision decision = executor.execute(
                    decoded.envelope(),
                    decoded.consumeContext(),
                    handler);
            return ProviderConsumeResult.of(decision);
        } catch (RuntimeException exception) {
            return ProviderConsumeResult.retry(
                    ConsumeFailureType.DECODE_ERROR,
                    exception.getMessage());
        }
    }
}
