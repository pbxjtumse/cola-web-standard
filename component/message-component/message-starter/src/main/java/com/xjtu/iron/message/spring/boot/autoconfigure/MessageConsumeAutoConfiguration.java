package com.xjtu.iron.message.spring.boot.autoconfigure;

import com.xjtu.iron.message.core.MessageComponentOptions;
import com.xjtu.iron.message.core.codec.MessageWireCodec;
import com.xjtu.iron.message.core.consume.ConsumeExceptionClassifier;
import com.xjtu.iron.message.core.consume.DefaultConsumeExceptionClassifier;
import com.xjtu.iron.message.core.consume.MessageConsumeExecutor;
import com.xjtu.iron.message.core.consume.MessageConsumerAdapter;
import com.xjtu.iron.message.core.consume.handler.MessageHandlerInvoker;
import com.xjtu.iron.message.core.consume.idempotency.*;
import com.xjtu.iron.message.core.consume.strategy.DefaultIdempotencyStrategy;
import com.xjtu.iron.message.core.consume.strategy.NoopTransactionStrategy;
import com.xjtu.iron.message.core.consume.transaction.MessageConsumeTransactionExecutor;
import com.xjtu.iron.message.core.consume.transaction.NoopMessageConsumeTransactionExecutor;
import com.xjtu.iron.message.core.context.MessageContextAccessor;
import com.xjtu.iron.message.spring.boot.autoconfigure.properties.MessageProperties;
import com.xjtu.iron.message.spring.boot.autoconfigure.properties.consume.Idempotency.MessageConsumeIdempotencyProperties;
import com.xjtu.iron.message.spring.boot.autoconfigure.properties.consume.MessageConsumeTransactionProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
    public MessageIdempotencyContextFactory messageIdempotencyContextFactory(MessageComponentOptions options) {
        return new DefaultMessageIdempotencyContextFactory(
                new DefaultMessageIdempotencySceneResolver(),
                new DefaultMessageIdempotencyKeyResolver(),
                new MessageIdempotencyOwnerTokenGenerator(),
                options.clock());
    }

    @Bean
    @ConditionalOnBean(MessageIdempotentOperations.class)
    @ConditionalOnMissingBean
    public MessageIdempotencyStateManager messageIdempotencyStateManager(
            MessageIdempotentOperations operations) {
        return new DefaultMessageIdempotencyStateManager(operations);
    }

    @Bean
    @ConditionalOnBean(MessageIdempotencyStateManager.class)
    @ConditionalOnMissingBean
    public MessageIdempotencyDecisionHandler messageIdempotencyDecisionHandler(
            MessageIdempotencyStateManager stateManager) {
        return new MessageIdempotencyDecisionHandler(
                stateManager);
    }

    /**
     * 当没有 idempotent-component 存储实现时，消费仍然可以启动，只是不启用真实幂等。
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageIdempotencyExecutor messageIdempotencyExecutor(
            ObjectProvider<MessageIdempotentOperations> operationsProvider,
            MessageIdempotencyContextFactory contextFactory,
            ObjectProvider<MessageIdempotencyStateManager> stateManagerProvider,
            ObjectProvider<MessageIdempotencyDecisionHandler> decisionHandlerProvider) {
        MessageIdempotentOperations operations = operationsProvider.getIfAvailable();
        if (operations == null) {
            return new NoopMessageIdempotencyExecutor();
        }
        MessageIdempotencyStateManager stateManager = stateManagerProvider.getIfAvailable();
        MessageIdempotencyDecisionHandler decisionHandler = decisionHandlerProvider.getIfAvailable();
        if (stateManager == null || decisionHandler == null) {
            return new NoopMessageIdempotencyExecutor();
        }
        return new DefaultMessageIdempotencyExecutor(
                contextFactory,
                stateManager,
                decisionHandler);
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
