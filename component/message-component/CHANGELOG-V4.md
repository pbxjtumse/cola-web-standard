# Message Component V4 变更记录

## 代码表达

- 将全部 Java record 类型转换为普通 final 不可变类。
- 显式声明 private final 字段。
- 显式展开构造器参数、校验、标准化和字段赋值。
- 保留原静态工厂、Builder、业务方法和 record 风格访问器，避免无关 API 破坏。
- 显式补充 equals、hashCode 和 toString。
- 保留并扩展类、属性、方法和内部中文注释。

## 错误修复

- 修复 `MessageWireCodec` 创建 `ConsumeContext` 时缺少投递次数参数的问题。
- 将失效的 `MessageWireMapper` 改为 `MessageWireCodec` 兼容适配器。
- 清除 `MessageWireMapper` 对旧 `MessageHeaders` 常量、`MessageCategory`、旧 SPI 方法的错误依赖。
- 移除普通类数组访问器上失效的 `@Override`。
- RocketMQ 和 Pulsar 配置对象的 `toString()` 不再输出敏感认证值。

## 验证

- Java 17、`-Xlint:all -Werror` 公共模块编译通过。
- InMemory 发送消费闭环通过。
- 父子消息 correlationId、causationId 传播验证通过。
- 消息模型契约验证通过。
- Kafka、RocketMQ、Pulsar 三个配置类语法编译检查通过。
