package com.xjtu.iron.transaction.starter.autoconfigure;

import com.xjtu.iron.transaction.api.event.TransactionEventListener;
import com.xjtu.iron.transaction.api.execution.TransactionExecutor;
import com.xjtu.iron.transaction.core.executor.DefaultTransactionExecutor;
import com.xjtu.iron.transaction.provider.spring.transaction.SpringTransactionProvider;
import com.xjtu.iron.transaction.spi.provider.TransactionProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

/**
 * 事务组件一期 Spring Boot 自动配置。
 *
 * <p>一期只接受单一候选 PlatformTransactionManager；多 TransactionManager 的选择和路由留到二期。</p>
 */
@AutoConfiguration(afterName = {
        "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration",
        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
@ConditionalOnClass(PlatformTransactionManager.class)
@ConditionalOnProperty(prefix = "iron.transaction", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnSingleCandidate(PlatformTransactionManager.class)
public class TransactionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TransactionProvider.class)
    public TransactionProvider transactionProvider(PlatformTransactionManager transactionManager) {
        // 将 Spring 的统一事务管理器包装成组件 SPI；ORM 和数据库类型都不进入 transaction-core。
        return new SpringTransactionProvider(transactionManager);
    }

    @Bean
    @ConditionalOnMissingBean(TransactionExecutor.class)
    public TransactionExecutor transactionExecutor(
            TransactionProvider provider,
            ObjectProvider<TransactionEventListener> listeners) {
        // 按 Spring Order 收集全部观测监听器，再创建唯一的 TransactionExecutor 门面。
        List<TransactionEventListener> orderedListeners = listeners.orderedStream().toList();
        return new DefaultTransactionExecutor(provider, orderedListeners);
    }
}
