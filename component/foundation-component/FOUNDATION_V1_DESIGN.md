# Foundation Component V1 详细设计

## 一、设计原则

### 1. 稳定语义才允许下沉

基础层只接收至少被多个技术组件复用、没有业务含义、不会反向依赖上层组件的能力。一个工具方法只在消息组件中使用，不代表它应该进入 Foundation。

### 2. 治理 Common，而不是消灭 Common

本版本不再把 Foundation 收缩为十几个类，但也不建立无边界的 `CommonUtils`。每个能力进入独立 Java 包，复杂能力进入独立 Maven 模块，并由 ArchUnit 防止架构腐化。

### 3. JDK 优先，开源库其次，自研最后

- JDK 已经稳定提供的类型直接使用，例如 `Instant`、`Duration`、`Clock`、`Base64`、`HexFormat`；
- Apache Commons 提供成熟实现时，以直接使用或薄语义层为主；
- 只有统一空值语义、Unicode 安全、资源限制、类型隔离等明确需求才自研。

### 4. Foundation 不判断组件失败语义

Foundation 只提供异常链解析和序列化异常。它不定义 `TRANSIENT`、`RETRYABLE` 或统一 `FailureCategory`。重试、消息、锁和事务组件必须根据各自操作语义分类失败。

## 二、模块职责

### foundation-core

包含 45 个生产类型，按 `text`、`collection`、`object`、`number`、`enumeration`、`function`、`validation`、`exception` 分包。

重点能力：Unicode 安全截断、命名格式转换、防御性集合分片、唯一索引、集合差异、树构建、安全数字转换、稳定编码枚举、受检异常函数接口、参数与状态校验、异步异常解包。

### foundation-time

基于 `ClockProvider` 提供可测试时间。包含 Deadline、Expiration、日期/时间/Instant 范围、时间窗口、Duration 解析、时间精度和时区转换。

该模块不处理节假日、交易日、账务日和清算日。

### foundation-id

定义字符串和长整型 ID 协议，提供 UUID、Compact UUID、前缀、组合及时间可排序 ID。当前时间可排序 ID 不是 Snowflake，不需要节点号，也不承诺连续性。

业务订单号、支付流水号和清算批次号仍由业务领域生成。

### foundation-codec

提供 Base64、Hex、安全摘要、Checksum、URL 编码、严格 Charset、基本类型字节转换、受限 GZIP 和内容指纹。

它不处理密码哈希、AES/RSA、签名和密钥管理。

### foundation-context

定义类型安全且不可变的 `ExecutionContext`，以及可跨 HTTP Header、消息 Header、任务元数据传播的 `ContextCarrier` 和 `ContextCodec`。

第一版明确不持有 ThreadLocal。后续可增加 `foundation-context-micrometer` 或由并行组件提供传播实现。

### foundation-reflection

提供类型、泛型、构造器、方法、注解、属性和字段访问。它不提供万能 Bean Copy，也不进行类路径扫描。

### foundation-resource

提供 classpath、文件系统和内存资源。读取时必须声明最大字节数，防止配置错误或恶意资源占满内存。

### foundation-serialization

`foundation-serialization-api` 定义稳定契约；`foundation-serialization-jackson` 提供 Jackson 2.x 实现。

Jackson 实现具有以下约束：

1. 构造时复制 ObjectMapper；
2. 默认注册 Java Time；
3. 默认忽略未知字段；
4. 不启用 default typing；
5. 支持泛型目标类型；
6. 尊重调用方 Charset；
7. 对序列化和反序列化内容执行最大字节限制；
8. 异常携带操作阶段、目标类型和内容长度，但不判断是否可重试。

## 三、依赖规则

```text
foundation-core                 -> Commons Lang / Collections
foundation-time                 -> foundation-core
foundation-id                   -> foundation-core + foundation-time
foundation-codec                -> foundation-core + Commons Codec
foundation-context              -> foundation-core
foundation-reflection           -> foundation-core
foundation-resource             -> foundation-core + Commons IO
foundation-serialization-api    -> foundation-core + foundation-reflection
foundation-serialization-jackson-> serialization-api + Jackson
foundation-test-support         -> 上述公共 API
```

任何 Foundation 模块都不允许依赖 message、cache、retry、lock、transaction 等上层组件。

## 四、第一版边界

第一版已经形成可用底座，但以下能力有意延后：

- Spring Boot AutoConfiguration 和 Starter；
- Micrometer Context Propagation；
- Reactor Context；
- Protobuf、Avro、Kryo 序列化实现；
- Snowflake 节点协调；
- 文件通配符扫描；
- 表达式引擎；
- 安全加密组件；
- 业务日历。

这些能力只有在真实组件集成提出明确需求后再增加，不能预先塞入基础层。
