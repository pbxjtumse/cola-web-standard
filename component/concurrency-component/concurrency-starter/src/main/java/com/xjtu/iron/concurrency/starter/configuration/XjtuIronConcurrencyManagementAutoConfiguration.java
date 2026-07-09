package com.xjtu.iron.concurrency.starter.configuration;

import com.xjtu.iron.concurrency.starter.endpoint.IronConcurrencyReadOnlyController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

/**
 * 并发组件只读管理接口自动装配。
 */
@AutoConfiguration(after = XjtuIronConcurrencyExecutionAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.web.bind.annotation.RestController")
@ConditionalOnProperty(prefix = "xjtu.iron.concurrency.management", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(IronConcurrencyReadOnlyController.class)
public class XjtuIronConcurrencyManagementAutoConfiguration {
}
