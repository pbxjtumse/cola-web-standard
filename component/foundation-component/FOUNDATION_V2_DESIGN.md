# Foundation Component V2 设计说明

## 1. 为什么重做这一版

上一版 Foundation 覆盖面很大，但是出现了明显问题：

- 通用工具拆得过碎；
- 一个 collection 包下出现大量小类；
- 使用者不知道该找哪个类；
- 自研代码太多，反而不如成熟开源工具踏实；
- 基础组件看起来像“设计练习”，不像工程里好用的 common 基础层。

V2 的核心修正是：

> 成熟通用能力优先复用开源库；Foundation 只提供薄门面和统一语义。

## 2. 哪些类保留模型命名

以下属于模型或能力抽象，不使用 Utils 后缀：

- `Deadline`
- `DateRange`
- `TimeRange`
- `InstantRange`
- `Expiration`
- `IdGenerator`
- `ExecutionContext`
- `ContextKey`
- `Resource`
- `ResourceLoader`
- `Serializer`
- `SerializedPayload`

它们本身表达稳定概念，不应该退化为工具方法集合。

## 3. 哪些类使用 Utils 后缀

以下属于静态工具门面，统一使用 Utils 后缀：

- `StringUtils`
- `MaskingUtils`
- `CollectionUtils`
- `ListUtils`
- `MapUtils`
- `SetUtils`
- `TreeUtils`
- `NumberUtils`
- `EnumUtils`
- `ExceptionUtils`
- `Base64Utils`
- `HexUtils`
- `DigestUtils`
- `ChecksumUtils`
- `UrlCodecUtils`
- `GzipUtils`
- `DateUtils`
- `DurationUtils`
- `InstantUtils`
- `TimeFormatUtils`
- `ClassUtils`
- `AnnotationUtils`
- `ConstructorUtils`
- `MethodUtils`
- `FieldUtils`
- `GenericTypeUtils`
- `ResourceUtils`

这些类是统一入口，不鼓励继续无限增加方法。每个 Utils 只保留跨组件高频能力。

## 4. 开源库使用原则

V2 引入或预期由上层父 POM 管理的开源库：

| 能力 | 开源库 | 使用方式 |
|---|---|---|
| 字符串、异常、反射 | Apache Commons Lang | 薄封装 |
| 集合 | Apache Commons Collections | 薄封装 |
| 编解码和摘要 | JDK 17 + Commons Codec | 优先 JDK，必要时 Commons |
| GZIP/IO 辅助 | JDK 17 + Commons IO | 薄封装 |
| JSON | Jackson | 独立实现模块 |
| 架构测试 | ArchUnit | 测试模块 |

Foundation 不引入 Hutool、Guava、Vavr、Eclipse Collections。不是这些库不好，而是它们会改变基础层依赖面或编程模型。

## 5. 不包含的内容

Foundation V2 不包含：

- 业务常量；
- 业务错误码；
- 业务 Result；
- Redis/Kafka/HTTP/DB 工具；
- SpringContextHolder；
- 线程池；
- 重试策略；
- 分布式锁状态；
- 幂等状态机；
- 密钥管理、签名、密码哈希。

这些属于上层技术组件或业务组件。
