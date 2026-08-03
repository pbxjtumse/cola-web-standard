# foundation-id 一期终版

`foundation-id` 为消息、重试、任务、Outbox、分布式锁等上层组件提供无业务语义的技术 ID 契约和本地生成算法。

## 一期边界

一期包含：

- `IdGenerator<T>`、`StringIdGenerator`、`LongIdGenerator`；
- UUID v4；
- RFC 9562 UUID v7；
- 单调 ULID；
- Nano ID；
- 显式 workerId 的 Snowflake；
- 前缀装饰器；
- 多技术片段组合装饰器；
- 按名称选择字符串生成器的不可变 Registry；
- 静态工厂 `IdGenerators`。

一期不包含：

- Spring Boot 自动配置；
- 根据 IP、MAC、Pod 名称自动推导 Snowflake workerId；
- Redis、ZooKeeper、数据库号段；
- UUID v5 等确定性 ID；
- 携带任意业务属性的通用生成上下文；
- 全局严格递增或跨 JVM 严格单调承诺。

## 包结构

```text
com.xjtu.iron.foundation.id
├── api
├── factory
├── registry
├── decorator
├── uuid
├── ulid
├── nanoid
└── snowflake
```

根包只保留 `package-info.java`，不存在两套同名接口。

## 推荐选择

| 场景 | 推荐算法 |
|---|---|
| retryId、messageId、eventId、executionId | UUID v7 |
| 日志、对象路径、紧凑有序字符串 | ULID |
| 外部分享链接、邀请码 | Nano ID |
| 兼容传统 UUID 接口 | UUID v4 |
| 数据库 BIGINT 主键 | Snowflake |

## 使用示例

```java
import com.xjtu.iron.foundation.id.api.StringIdGenerator;
import com.xjtu.iron.foundation.id.factory.IdGenerators;

StringIdGenerator generator = IdGenerators.uuidV7();
String id = generator.nextId();
```

按名称选择：

```java
import com.xjtu.iron.foundation.id.registry.StringIdGeneratorRegistry;

StringIdGeneratorRegistry registry = StringIdGeneratorRegistry.builder()
        .register("retry", IdGenerators.uuidV7())
        .register("message", IdGenerators.uuidV7())
        .register("object-path", IdGenerators.ulid())
        .build();

String retryId = registry.require("retry").nextId();
```

`foundation-id` 不定义 `retry`、`message` 等上层领域常量；名称由对应组件负责声明。

## Snowflake 注意事项

Snowflake 的 `workerId` 必须由部署系统保证唯一。Kubernetes 中优先考虑 StatefulSet ordinal 或独立租约分配，不要直接使用随机数、IP、MAC 地址或哈希碰撞不可控的 Pod 名称。
