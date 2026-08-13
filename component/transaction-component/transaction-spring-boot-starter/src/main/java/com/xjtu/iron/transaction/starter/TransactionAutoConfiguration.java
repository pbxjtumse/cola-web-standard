package com.xjtu.iron.transaction.starter;

import com.xjtu.iron.transaction.api.TransactionEventListener;
import com.xjtu.iron.transaction.api.TransactionExecutor;
import com.xjtu.iron.transaction.core.DefaultTransactionExecutor;
import com.xjtu.iron.transaction.provider.spring.SpringTransactionProvider;
import com.xjtu.iron.transaction.spi.TransactionProvider;
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
 * 事务组件一期自动配置。
 *
 * <p>一期明确只支持单一候选 PlatformTransactionManager。
 * 多 TransactionManager 的显式选择、路由和命名策略放到二期。</p>
 */
@AutoConfiguration
@ConditionalOnClass(PlatformTransactionManager.class)
@ConditionalOnProperty(
        prefix = "iron.transaction",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@ConditionalOnSingleCandidate(PlatformTransactionManager.class)
public class TransactionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TransactionProvider.class)
    public TransactionProvider transactionProvider(
            PlatformTransactionManager transactionManager) {
        return new SpringTransactionProvider(transactionManager);
    }

    @Bean
    @ConditionalOnMissingBean(TransactionExecutor.class)
    public TransactionExecutor transactionExecutor(
            TransactionProvider provider,
            ObjectProvider<TransactionEventListener> listeners) {
        List<TransactionEventListener> orderedListeners = listeners.orderedStream().toList();
        return new DefaultTransactionExecutor(provider, orderedListeners);
    }
}
