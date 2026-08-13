package com.xjtu.iron.transaction.provider.spring;

import com.xjtu.iron.transaction.api.TransactionIsolation;
import com.xjtu.iron.transaction.api.TransactionOptions;
import com.xjtu.iron.transaction.api.TransactionPropagation;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.time.Duration;

/**
 * 稳定 API 到 Spring TransactionDefinition 的映射器。
 */
final class SpringTransactionDefinitionMapper {

    private SpringTransactionDefinitionMapper() {
    }

    static DefaultTransactionDefinition map(TransactionOptions options) {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setName(options.name());
        definition.setReadOnly(options.readOnly());
        definition.setPropagationBehavior(mapPropagation(options.propagation()));
        definition.setIsolationLevel(mapIsolation(options.isolation()));

        if (options.timeout() != null) {
            definition.setTimeout(toSpringTimeoutSeconds(options.timeout()));
        }
        return definition;
    }

    private static int mapPropagation(TransactionPropagation propagation) {
        return switch (propagation) {
            case REQUIRED -> TransactionDefinition.PROPAGATION_REQUIRED;
            case REQUIRES_NEW -> TransactionDefinition.PROPAGATION_REQUIRES_NEW;
            case MANDATORY -> TransactionDefinition.PROPAGATION_MANDATORY;
        };
    }

    private static int mapIsolation(TransactionIsolation isolation) {
        return switch (isolation) {
            case DEFAULT -> TransactionDefinition.ISOLATION_DEFAULT;
            case READ_UNCOMMITTED -> TransactionDefinition.ISOLATION_READ_UNCOMMITTED;
            case READ_COMMITTED -> TransactionDefinition.ISOLATION_READ_COMMITTED;
            case REPEATABLE_READ -> TransactionDefinition.ISOLATION_REPEATABLE_READ;
            case SERIALIZABLE -> TransactionDefinition.ISOLATION_SERIALIZABLE;
        };
    }

    /**
     * Spring 的传统事务超时单位是整秒。
     * 对小于 1 秒的正 Duration 向上取整为 1 秒，避免被截断成 0。
     */
    private static int toSpringTimeoutSeconds(Duration timeout) {
        long millis = timeout.toMillis();
        long seconds = Math.max(1L, (millis + 999L) / 1000L);
        if (seconds > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("transaction timeout is too large: " + timeout);
        }
        return (int) seconds;
    }
}
