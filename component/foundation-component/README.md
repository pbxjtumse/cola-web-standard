# Foundation Component V1

## 1. 定位

Foundation Component 是缓存、消息、重试、分布式锁、幂等、事务、并行、治理和可观测性组件共同依赖的底层技术组件。

它提供：

- 纯技术文本、集合、数值、枚举、校验和异常链工具；
- 可测试的时间模型；
- 技术 ID 生成协议和本地实现；
- 编解码、摘要、校验和及受限压缩；
- 不绑定 ThreadLocal 的执行上下文模型；
- 受控反射能力；
- classpath、文件和内存资源读取；
- 序列化抽象和 Jackson JSON 实现；
- 跨组件测试支持和 ArchUnit 架构约束。

它不提供：

- 业务错误码、业务日期、订单号和支付流水号；
- Redis、MQ、数据库、HTTP 等客户端工具；
- 万能 Bean Copy、全局 SpringContextHolder 或全局 ObjectMapper；
- 重试分类、消息 ACK、事务补偿和分布式调度；
- 密码存储、密钥管理、数字签名和加解密策略。

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

序列化在目录层面属于同一能力域，但 API 与 Jackson 仍是两个 Jar：上层技术组件只依赖 API，应用装配层再选择 Jackson 实现。

## 3. Java 与依赖

- Java 17；
- Apache Commons Lang 3.20.0；
- Apache Commons Collections 4.5.0；
- Apache Commons Codec 1.22.0；
- Apache Commons IO 2.22.0；
- Jackson 2.21.2 LTS；
- JUnit 6.1.2；
- ArchUnit 1.4.2。

这些版本应在你现有项目的根 BOM 中统一管理。当前 POM 内的版本用于让本工程能够独立构建。

## 4. 构建

```bash
mvn clean verify
```

## 5. 推荐依赖方式

消息 API 或消息核心只依赖序列化协议：

```xml
<dependency>
    <groupId>com.xjtu.iron</groupId>
    <artifactId>foundation-serialization-api</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

消息配置或应用装配层选择 Jackson：

```xml
<dependency>
    <groupId>com.xjtu.iron</groupId>
    <artifactId>foundation-serialization-jackson</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

测试模块按 test scope 引入：

```xml
<dependency>
    <groupId>com.xjtu.iron</groupId>
    <artifactId>foundation-test-support</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

## 6. 注释规则

- 公开类和核心协议使用类级 Javadoc；
- 公开业务方法说明语义、边界和返回规则；
- 关键分支说明为什么需要这样处理；
- 构造函数、简单 getter 和 setter 不添加重复说明；
- 不使用 `字段名 + 声明并保存内部状态` 一类无信息量注释。
