# Message Component V4 构建与验证报告

## 本次输入

- 基础工程：用户上传的最新 `message-component.zip`
- 改造目标：所有 Java record 转换为普通不可变类
- 约束：保留类、属性、方法和内部中文注释，不拆散工程，不删除 Provider、Demo、Testkit、文档和时序图

## 改造规模

- Maven POM：11 个
- Java 文件：61 个
- 原 record 类型：24 个
- V4 剩余 record 类型声明：0 个
- Java 代码行：8569 行
- Java 注释行：3189 行
- PlantUML 文件：14 个
- Markdown 文件：16 个

原始版本约有 1796 行 Java 注释；V4 没有执行“去注释”式转换，转换后注释行增加到 3189 行，新增内容主要用于显式字段、构造赋值、访问器和值对象方法说明。

## 已执行验证

1. 所有 11 个 `pom.xml` 通过 XML 解析。
2. 扫描全部 61 个 Java 文件，确认不存在 Java record 类型声明。
3. `message-api`、`message-spi`、`message-core`、`message-testkit`、`message-demo` 使用 Java 17、`-Xlint:all -Werror` 编译通过。
4. `InMemoryMessageDemo` 运行通过。
5. `MessageModelContractVerifier` 运行通过。
6. Kafka、RocketMQ、Pulsar 三个 Provider 配置类完成独立 Java 17 语法编译检查。
7. 14 个 PlantUML 文件完成图文件标记和相对 include 路径检查。
8. `MessageWireMapper` 不再引用旧 `MessageHeaders` 常量、旧 `MessageCategory` 字段和旧 `ProviderInboundMessage` 方法。
9. RocketMQ `secretKey`、`accessKey` 和 Pulsar `authenticationToken` 不再由配置对象 `toString()` 输出。

## 运行结果摘要

```text
root send status=CONFIRMED
order messageKey=order-10001
child send status=CONFIRMED
points messageKey=order-10001
points correlationId=order-flow-10001
points causationId=<parent messageId>
message model contract verification=PASSED
```

## MessageWireMapper 修复结论

原 `MessageWireMapper` 是旧模型残留，主要问题包括：

- 使用 `MessageHeaders.MESSAGE_ID` 等已经迁移到 `MessageHeaderNames` 的常量；
- 调用已经不存在的 `MessageDestination.category()`；
- 调用已经不存在的 `ProviderInboundMessage.key()`、`deliveryAttempt()`、`metadata()`；
- 与 `MessageWireCodec` 重复维护线级协议。

V4 将 `MessageWireCodec` 作为唯一真实实现，`MessageWireMapper` 改为带 `@Deprecated` 的兼容适配器：

```text
MessageWireMapper
    ↓ delegate
MessageWireCodec
```

这样既修复编译错误，也避免直接删除旧类导致已有调用方立即失效。

## 未完成验证

当前执行环境没有 Maven，且不能访问 Maven Central，因此没有完成 Kafka、RocketMQ、Pulsar 原生 SDK 依赖下的全模块 Maven 编译，也没有连接真实 Broker。

请在本地执行：

```bash
mvn clean verify
```

随后分别连接 Kafka、RocketMQ 5.x Proxy 和 Pulsar 测试集群，完成发送、消费、关闭、认证、网络异常和重平衡验证。V4 不能仅凭当前验证声明生产可用。
