# 一期候选版审查结论

## 一、结论

当前项目属于一期，并且已具备进入真实组件联调前的核心闭环。

可以验证：

- 同步异常重试；
- 返回结果重试；
- 总时长和最大次数；
- 退避和抖动；
- 中断与取消；
- 命名策略；
- Spring 自动配置；
- 事件和指标。

不能验证：

- Broker 最终是否收到消息；
- 服务重启后的重试恢复；
- 消息重复发送或重复消费；
- Outbox 事务一致性；
- 重试主题和死信；
- 多实例重试预算。

## 二、本次必须修复的问题

| 问题 | 风险 | 处理 |
|---|---|---|
| API 根包堆积 20 多个类型 | 使用者难以理解职责 | 按 execution、policy、backoff、event、exception 拆分 |
| 执行器测试整文件被注释 | Maven 显示成功但核心逻辑没有测试 | 恢复 14 个 Core 测试 |
| Java 21 文档和构建限制 | 与现有 Java 17 组件不一致 | 统一 `release=17` |
| Spring Map 元数据误识别 | IDEA 将 `TRANSIENT` 识别成整数 | 增加 additional metadata |
| RetryPolicy 内嵌大量规则执行代码 | 策略和值匹配职责混杂 | 提取 RuleBasedRetryClassifier |
| Executor 管理监听器复制和异常隔离 | 状态机职责过重 | 提取 RetryEventDispatcher |
| 压缩包包含 target、iml、__MACOSX | 污染仓库并造成旧 class 干扰 | 清理并增加自动检查 |
| README 版本与 Maven 版本不一致 | 容易错误发布 | 区分 Maven 统一版本和交付里程碑 |

## 三、仍建议在一期验收时观察的问题

### 1. DefaultRetryExecutor 仍然较长

它承担完整同步状态机，当前继续拆分会产生大量只包装一两个私有方法的类。已经提取事件分发后，剩余方法仍属于同一状态机。

验收阶段重点观察：

- 是否继续出现超过 6 个参数的私有方法；
- 新功能是否开始插入主循环；
- 二期异步能力是否试图复用同步循环。

如果二期加入异步，不应继续扩张当前类，而应引入独立的异步执行器和共享决策内核。

### 2. YAML 的 retry-on 共用一个失败分类

当前：

```yaml
retry-failure-category: TRANSIENT
retry-on:
  - java.io.IOException
  - java.util.concurrent.TimeoutException
```

适合相同语义的一组异常。不同语义异常不应混入同一个命名策略。

二期可以评估逐条规则配置，但一期不再增加配置复杂度。

### 3. maxDuration 不是单次强制超时

同步操作进入业务代码后无法被安全强杀。`maxDuration` 只控制是否启动下一次尝试和是否继续等待。

## 四、一期验收门槛

建议满足以下条件后冻结一期：

1. 完整父工程 `mvn -pl retry-component -am clean verify` 通过；
2. Java 17 环境通过；
3. 所有 27 个当前单元/上下文测试执行而非仅编译；
4. Demo 五个接口实际运行；
5. 指标能在 Actuator 中看到；
6. IDEA 不再把 `TRANSIENT` 标为整数；
7. 消息组件只依赖 retry-api 公共类型，不依赖 core 实现；
8. 完成一次异常、取消、中断和监听器失败的日志核对。

## RC2 基础能力复用

通用 ID 生成已迁移到 `foundation-id`，retry-component 不再维护 UUID 实现。
