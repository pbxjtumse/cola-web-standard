package com.xjtu.iron.message.core.consume;

import com.xjtu.iron.message.api.consume.context.ConsumeContext;
import com.xjtu.iron.message.api.consume.decision.ConsumeDecision;
import com.xjtu.iron.message.api.consume.handler.MessageHandler;
import com.xjtu.iron.message.api.consume.definition.ConsumerDefinition;
import com.xjtu.iron.message.api.model.MessageEnvelope;
import com.xjtu.iron.message.core.consume.idempotency.MessageIdempotencyExecutor;
import com.xjtu.iron.message.core.consume.idempotency.NoopMessageIdempotencyExecutor;

import java.util.Objects;

/**
 * 可靠消费主模板，固定“幂等包住业务 Handler”的执行顺序。
 */
public final class ConsumeExecutionTemplate {
    private final MessageIdempotencyExecutor idempotencyExecutor;
    private final ConsumeExceptionClassifier exceptionClassifier;

    public ConsumeExecutionTemplate() {
        this(new NoopMessageIdempotencyExecutor(), new DefaultConsumeExceptionClassifier());
    }

    public ConsumeExecutionTemplate(
            MessageIdempotencyExecutor idempotencyExecutor,
            ConsumeExceptionClassifier exceptionClassifier) {
        this.idempotencyExecutor = Objects.requireNonNull(idempotencyExecutor, "idempotencyExecutor must not be null");
        this.exceptionClassifier = Objects.requireNonNull(exceptionClassifier, "exceptionClassifier must not be null");
    }

    public <T> ConsumeDecision execute(ConsumerDefinition<T> definition, MessageEnvelope<T> message, ConsumeContext context, MessageHandler<T> handler) {
        Objects.requireNonNull(definition, "definition must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        return idempotencyExecutor.execute(message, context, definition.idempotencyOptions(), () -> invoke(message, context, handler));
    }

    private <T> ConsumeDecision invoke(MessageEnvelope<T> message, ConsumeContext context, MessageHandler<T> handler) {
        try {
            ConsumeDecision decision = handler.handle(message, context);
            return decision == null ? ConsumeDecision.RETRY : decision;
        } catch (RuntimeException exception) {
            return exceptionClassifier.classify(exception, context);
        }
    }
}
