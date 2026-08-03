# 开源 Common Util 选型

## 第一版正式使用

### Apache Commons Lang

用于成熟的字符串空白与标准化实现。本工程不再包装其全部 API，只在需要统一组件语义时建立薄层。

### Apache Commons Collections

纳入依赖管理，供后续高级集合能力使用。第一版大部分集合实现仍基于 Java 17，以减少不必要的 API 暴露。

### Apache Commons Codec

用于 SHA-256、SHA-512 等成熟摘要实现；Base64 和 Hex 优先使用 Java 17 原生 API。

### Apache Commons IO

资源模块纳入依赖管理。资源读取使用 `BoundedInputStream` 在流式读取阶段限制最大字节数，并在到达上限后拒绝继续加载，避免先把完整资源读入内存再判断。

### Jackson 2.21 LTS

作为 JSON 默认实现。API 层完全不知道 Jackson，避免消息、缓存和 Outbox 直接绑定 ObjectMapper。

### JUnit 与 ArchUnit

JUnit 负责行为测试，ArchUnit 负责依赖方向和实现泄漏检查。

## 暂不作为底层强制依赖

- Hutool：覆盖过广，容易形成多套相同能力；
- Guava：Java 17 已覆盖大量常用场景，真正需要 Multimap、Graph 时由具体组件引入；
- Commons Text：模板、转义和文本相似度不是所有技术组件的共同需求；
- Vavr、Eclipse Collections：会改变整个工程的编程模型；
- Spring Core Utilities：可以参考，但纯 Foundation 不应绑定 Spring。
