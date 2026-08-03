# Foundation Component V1

## 1. 定位

Foundation Component 是缓存、消息、重试、分布式锁、幂等、事务、并行、治理和可观测性组件共同依赖的底层技术组件。

它只提供跨多个技术组件复用、没有业务含义、不会反向依赖上层组件的能力。业务订单号、支付流水号、清算批次号、业务错误码和失败重试分类不属于 Foundation。

## 2. 模块结构

```text
foundation-component
├── foundation-core
├── foundation-time
├── foundation-id
├── foundation-codec
├── foundation-context
├── foundation-reflection
├── foundation-resource
├── foundation-serialization
│   ├── foundation-serialization-api
│   └── foundation-serialization-jackson
├── foundation-test-support
└── foundation-architecture-tests
```

## 3. foundation-id 当前能力

`foundation-id` 统一提供：

- `IdGenerator<T>`、`StringIdGenerator`、`LongIdGenerator`；
- UUID v4；
- RFC 9562 UUID v7；
- 单调 ULID；
- Nano ID；
- 显式 workerId 的 64 位 Snowflake；
- Compact UUID v4；
- 固定前缀和组合生成器；
- 命名生成器注册表；
- 面向旧代码的一期兼容入口。

推荐选择：

| 场景 | 推荐算法 |
|---|---|
| 重试执行 ID、事件 ID、请求 ID | UUID v7 |
| 日志、对象路径、紧凑时间有序字符串 | ULID |
| 外部分享链接、邀请码、短资源 ID | Nano ID |
| 数据库 `BIGINT` 主键 | Snowflake |
| 兼容已有随机 UUID 协议 | UUID v4 |

`foundation-id` 不提供 Snowflake workerId 的自动分配。Kubernetes、Redis、ZooKeeper 或数据库号段协调应由后续独立集成模块承担，不能在纯算法模块里通过 IP、MAC 或随机值猜测节点号。

## 4. 使用示例

```java
StringIdGenerator retryIdGenerator = IdGenerators.uuidV7();
String retryId = retryIdGenerator.nextId();

StringIdGenerator objectIdGenerator = IdGenerators.ulid();
String objectId = objectIdGenerator.nextId();

LongIdGenerator databaseIdGenerator = IdGenerators.snowflake(7L);
long databaseId = databaseIdGenerator.nextLongId();
```

业务需要稳定前缀时：

```java
StringIdGenerator generator = IdGenerators.prefixed(
        "retry",
        "-",
        IdGenerators.uuidV7()
);
```

## 5. 依赖方式

上层组件只依赖实际使用的 Jar，不依赖 `foundation-component` 聚合 POM：

```xml
<dependency>
    <groupId>com.xjtu.iron</groupId>
    <artifactId>foundation-id</artifactId>
</dependency>
```

测试代码需要固定或顺序 ID 时：

```xml
<dependency>
    <groupId>com.xjtu.iron</groupId>
    <artifactId>foundation-test-support</artifactId>
    <scope>test</scope>
</dependency>
```

## 6. Java 与框架边界

- Java 17；
- `foundation-id` 不依赖 Spring、Micrometer、Jackson、消息客户端或数据库；
- Foundation 第一版不提供全局 Spring Boot ID Bean；
- 每个上层组件在自己的装配模块中选择专用生成器 Bean，避免消息 ID、重试 ID、任务 ID 因同类型 Bean 相互冲突。

## 7. 构建

```bash
python scripts/verify-id-layout.py
mvn clean verify
```

## 8. 注释规则

- 公开类、核心协议和关键字段说明真实语义；
- 核心分支说明为什么需要这样处理；
- 构造器、简单 getter/setter、import 和普通赋值不写机械注释；
- 不允许把每一行代码翻译成无信息量中文。
