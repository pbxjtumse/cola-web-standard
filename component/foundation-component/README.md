# foundation-component V2 thin facade

这是 Foundation Component 的第二版重构版本。

本版设计目标不是重新发明一套巨大的 Common Util，而是：

1. 对成熟开源工具做薄封装；
2. 保留统一包名、统一命名和统一边界；
3. 模型类和能力接口保持清晰语义；
4. 静态工具门面统一使用 `XxxUtils`；
5. 删除上一版过度细碎的工具类拆分。

## 模块

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

## 代码规模

- POM：13 个
- 生产 Java：106 个
- 测试 Java：7 个

## 命名规则

- 值对象和模型：不加 Utils，例如 `Deadline`、`DateRange`、`ExecutionContext`、`SerializedPayload`。
- 能力接口：不加 Utils，例如 `IdGenerator`、`Serializer`、`ResourceLoader`。
- 静态工具门面：使用 `XxxUtils`，例如 `StringUtils`、`CollectionUtils`、`DigestUtils`。

## 构建

放入现有 `component` 工程后执行：

```bash
mvn -U -pl component/foundation-component -am clean verify
```

或在包含根父 POM 的项目根目录执行：

```bash
mvn -U clean verify
```

当前生成环境没有 Maven，因此只完成了 POM 解析和 Java 17 语法级编译检查。完整构建仍需在本地 Maven 环境执行。
