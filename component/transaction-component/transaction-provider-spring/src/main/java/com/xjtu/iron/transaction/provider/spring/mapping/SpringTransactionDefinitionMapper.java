package com.xjtu.iron.transaction.provider.spring.mapping;

import com.xjtu.iron.transaction.api.definition.TransactionIsolation;
import com.xjtu.iron.transaction.api.definition.TransactionOptions;
import com.xjtu.iron.transaction.api.definition.TransactionPropagation;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.time.Duration;

/**
 * 稳定 TransactionOptions 到 Spring TransactionDefinition 的映射器。
 */
public final class SpringTransactionDefinitionMapper {

    private SpringTransactionDefinitionMapper() {
    }

    public static DefaultTransactionDefinition map(TransactionOptions options) {
        // 1. 创建 Spring 事务定义，只在 Provider 层出现 Spring 类型。
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();

        // 2. 映射通用事务属性；core/api 不直接依赖 Spring 常量。
        definition.setName(options.name());
        definition.setReadOnly(options.readOnly());
        definition.setPropagationBehavior(mapPropagation(options.propagation()));
        definition.setIsolationLevel(mapIsolation(options.isolation()));

        // 3. Spring 传统事务超时使用整秒；没有配置时保持底层事务管理器默认值。
        if (options.timeout() != null) {
            definition.setTimeout(toSpringTimeoutSeconds(options.timeout()));
        }

        return definition;
    }

    private static int mapPropagation(TransactionPropagation propagation) {
        // 将组件自己的稳定枚举转换为 Spring propagation 常量。
        return switch (propagation) {
            case REQUIRED -> TransactionDefinition.PROPAGATION_REQUIRED;
            case REQUIRES_NEW -> TransactionDefinition.PROPAGATION_REQUIRES_NEW;
            case MANDATORY -> TransactionDefinition.PROPAGATION_MANDATORY;
        };
    }

    private static int mapIsolation(TransactionIsolation isolation) {
        // 将组件自己的隔离级别映射给 Spring，最终支持程度仍由具体数据库/事务管理器决定。
        return switch (isolation) {
            case DEFAULT -> TransactionDefinition.ISOLATION_DEFAULT;
            case READ_UNCOMMITTED -> TransactionDefinition.ISOLATION_READ_UNCOMMITTED;
            case READ_COMMITTED -> TransactionDefinition.ISOLATION_READ_COMMITTED;
            case REPEATABLE_READ -> TransactionDefinition.ISOLATION_REPEATABLE_READ;
            case SERIALIZABLE -> TransactionDefinition.ISOLATION_SERIALIZABLE;
        };
    }

    private static int toSpringTimeoutSeconds(Duration timeout) {
        // 小于 1 秒的正 Duration 向上取整为 1 秒，防止 toSeconds() 截断为 0。
        long millis = timeout.toMillis();
        long seconds = Math.max(1L, (millis + 999L) / 1000L);

        // Spring TransactionDefinition 超时字段是 int 秒，过大值必须显式拒绝而不是溢出。
        if (seconds > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("transaction timeout is too large: " + timeout);
        }

        return (int) seconds;
    }
}
