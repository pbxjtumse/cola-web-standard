package com.xjtu.iron.message.core.consume;

import com.xjtu.iron.message.api.consume.ConsumerDefinition;
import com.xjtu.iron.message.api.model.MessageDestination;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存 Map 的消费者定义注册表。
 */
public final class DefaultConsumerDefinitionRegistry implements ConsumerDefinitionRegistry {
    private final Map<String, ConsumerDefinition<?>> definitions = new ConcurrentHashMap<>();

    @Override
    public void register(ConsumerDefinition<?> definition) {
        Objects.requireNonNull(definition, "definition must not be null");
        definitions.put(key(definition.destination(), definition.consumerGroup()), definition);
    }

    @Override
    public Optional<ConsumerDefinition<?>> find(MessageDestination destination, String consumerGroup) {
        if (destination == null || consumerGroup == null || consumerGroup.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(definitions.get(key(destination, consumerGroup)));
    }

    private static String key(MessageDestination destination, String consumerGroup) {
        return destination.qualifiedName() + "::" + consumerGroup.trim();
    }
}
