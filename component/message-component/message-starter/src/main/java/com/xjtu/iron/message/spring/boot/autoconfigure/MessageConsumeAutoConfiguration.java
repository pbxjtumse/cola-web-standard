package com.xjtu.iron.message.spring.boot.autoconfigure;

import com.xjtu.iron.message.core.MessageComponentOptions;
import com.xjtu.iron.message.core.codec.MessageWireCodec;
import com.xjtu.iron.message.core.consume.ConsumeExceptionClassifier;
import com.xjtu.iron.message.core.consume.DefaultConsumeExceptionClassifier;
import com.xjtu.iron.message.core.consume.MessageConsumeExecutor;
import com.xjtu.iron.message.core.consume.MessageConsumerAdapter;
import com.xjtu.iron.message.core.consume.handler.MessageHandlerInvoker;
import com.xjtu.iron.message.core.consume.idempotency.DefaultMessageIdempotencyExecutor;
import com.xjtu.iron.message.core.consume.idempotency.DefaultMessageIdempotencyKeyResolver;
import com.xjtu.iron.message.core.consume.idempotency.DefaultMessageIdempotencySceneResolver;
import com.xjtu.iron.message.core.consume.idempotency.MessageIdempotencyExecutor;
import com.xjtu.iron.message.core.consume.idempotency.MessageIdempotencyOwnerTokenGenerator;
import com.xjtu.iron.message.core.consume.idempotency.MessageIdempotentOperations;
import com.xjtu.iron.message.core.consume.idempotency.NoopMessageIdempotencyExecutor;
import com.xjtu.iron.message.core.consume.strategy.DefaultIdempotencyStrategy;
import com.xjtu.iron.message.core.consume.strategy.NoopTransactionStrategy;
import com.xjtu.iron.message.core.consume.transaction.MessageConsumeTransactionExecutor;
import com.xjtu.iron.message.core.consume.transaction.NoopMessageConsumeTransactionExecutor;
import com.xjtu.iron.message.core.context.MessageContextAccessor;
import com.xjtu.iron.message.spring.boot.autoconfigure.properties.MessageProperties;
import com.xjtu.iron.message.spring.boot.autoconfigure.properties.consume.MessageConsumeTransactionProperties;
import com.xjtu.iron.message.spring.boot.autoconfigure.properties.consume.idempotency.MessageConsumeIdempotencyProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 消息消费自动配置。
 */
@AutoConfiguration(after = MessageCoreAutoConfiguration.class)
@EnableConfigurationProperties(MessageProperties.class)
@ConditionalOnProperty(prefix = "xjtu.iron.message", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MessageConsumeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ConsumeExceptionClassifier consumeExceptionClassifier() {
        return new DefaultConsumeExceptionClassifier();
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageConsumeTransactionExecutor messageConsumeTransactionExecutor(MessageProperties properties) {
        MessageConsumeTransactionProperties transaction = properties.getConsume().getTransaction();
        if (transaction.isEnabled() && transaction.isRequired()) {
            throw new IllegalStateException(
                    "xjtu.iron.message.consume.transaction.enabled=true and required=true, "
                            + "but MessageConsumeTransactionExecutor is missing. "
                            + "Please add a transaction integration bean or disable required.");
        }
        return new NoopMessageConsumeTransactionExecutor();
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageIdempotencyExecutor messageIdempotencyExecutor(
            MessageProperties properties,
            ObjectProvider<MessageIdempotentOperations> operationsProvider,
            MessageConsumeTransactionExecutor transactionExecutor,
            MessageComponentOptions options) {
        MessageConsumeIdempotencyProperties idempotency = properties.getConsume().getIdempotency();
        MessageIdempotentOperations operations = operationsProvider.getIfAvailable();
        if (operations == null) {
            if (idempotency.isEnabled()) {
                throw new IllegalStateException(
                        "xjtu.iron.message.consume.idempotency.enabled=true, "
                                + "but MessageIdempotentOperations is missing. "
                                + "Please add idempotent integration or disable consume idempotency.");
            }
            return new NoopMessageIdempotencyExecutor();
        }
        return new DefaultMessageIdempotencyExecutor(
                operations,
                new DefaultMessageIdempotencySceneResolver(),
                new DefaultMessageIdempotencyKeyResolver(),
                new MessageIdempotencyOwnerTokenGenerator(),
                transactionExecutor,
                options.clock());
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageConsumeExecutor messageConsumeExecutor(
            MessageIdempotencyExecutor idempotencyExecutor,
            ConsumeExceptionClassifier exceptionClassifier) {
        return new MessageConsumeExecutor(
                new DefaultIdempotencyStrategy(idempotencyExecutor),
                new NoopTransactionStrategy(),
                new MessageHandlerInvoker(exceptionClassifier));
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageConsumerAdapter messageConsumerAdapter(
            MessageWireCodec wireCodec,
            MessageConsumeExecutor consumeExecutor,
            MessageContextAccessor contextAccessor) {
        return new MessageConsumerAdapter(wireCodec, consumeExecutor, contextAccessor);
    }
}
