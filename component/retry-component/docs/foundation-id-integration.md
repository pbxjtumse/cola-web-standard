# retry-component 与 Foundation ID 一期终版集成

## 一、最终依赖

```text
retry-api
    不依赖 foundation-id

retry-core
    -> retry-api
    -> foundation-id

retry-config
    -> retry-core
    -> foundation-id
    -> Spring Boot
```

公共类型统一使用：

```java
import com.xjtu.iron.foundation.id.api.StringIdGenerator;
import com.xjtu.iron.foundation.id.factory.IdGenerators;
import com.xjtu.iron.foundation.id.registry.StringIdGeneratorRegistry;
```

不再使用已经删除的 Foundation 根包重复接口。

## 二、ID 选择顺序

执行器解析 `retryId` 的顺序为：

```text
RetryExecution 显式 retryId
    ↓ 未提供
注入的 StringIdGenerator
```

Spring 自动配置创建生成器 Bean 时顺序为：

```text
应用自定义 retryIdGenerator Bean
    ↓ 未提供
StringIdGeneratorRegistry 中名称 retry
    ↓ Registry 不存在
Foundation UUID v7
```

显式业务关联 ID 始终优先。自动生成值仅用于技术链路关联，不等于幂等键。

## 三、普通 Java 使用

```java
StringIdGenerator generator = IdGenerators.uuidV7();

RetryExecutor executor = new DefaultRetryExecutor(
        policyRegistry,
        listeners,
        sleeper,
        clock,
        generator
);
```

默认构造器同样使用 Foundation UUID v7，不在重试组件中维护 UUID 实现。

## 四、Spring 专用 Bean

```text
retryIdGenerator
```

应用可以直接覆盖：

```java
@Bean("retryIdGenerator")
public StringIdGenerator retryIdGenerator() {
    return IdGenerators.ulid();
}
```

也可以提供 Foundation Registry：

```java
@Bean
public StringIdGeneratorRegistry stringIdGeneratorRegistry() {
    return StringIdGeneratorRegistry.builder()
            .register("retry", IdGenerators.uuidV7())
            .register("message", IdGenerators.uuidV7())
            .register("object-path", IdGenerators.ulid())
            .build();
}
```

专用 Bean 的优先级高于 Registry。

## 五、边界

Foundation ID 不负责：

- 业务幂等；
- 消息只发送一次；
- Outbox 原子性；
- Snowflake workerId 协调；
- 重试任务持久化。
