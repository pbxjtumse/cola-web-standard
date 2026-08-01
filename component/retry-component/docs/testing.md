# 测试与验证策略

## 一、API 模型测试

必须覆盖：

1. `RetryDecision` 类型、分类和延迟不变量。
2. `RetryDelay` 允许显式零等待并保留来源。
3. `RetryAttempt` 的返回值与异常互斥。
4. `RetryResult` 状态、值、异常和尝试次数一致性。
5. 属性映射防御性复制。
6. 非幂等 REJECT 模式。
7. 声明式规则与自定义分类器禁止混用。
8. 同一异常类型跨动作冲突。
9. 同一动作最具体异常规则优先。
10. cause 最大深度和循环链终止。
11. 固定种子 Full Jitter 可复现。

## 二、执行器测试

必须覆盖：

1. 第一次成功。
2. 多次异常后成功。
3. 异常重试耗尽。
4. 返回结果触发重试。
5. 默认不可重试。
6. STOP 与 ABORT 最终状态不同。
7. 服务端延迟覆盖默认退避。
8. 等待将越过总预算时直接超时。
9. 业务操作抛 `InterruptedException`。
10. `RetrySleeper` 被中断并恢复线程标记。
11. `Error` 直接传播。
12. 分类器抛异常。
13. 分类器把失败判为成功。
14. 退避策略抛异常或返回 null。
15. 监听器异常隔离。
16. 开始前取消。
17. 第一次失败后取消。
18. 等待后取消。
19. 自定义 `retryId` 保留。
20. 自定义 Clock/Sleeper/ID Generator。

## 三、Registry 测试

- 新增策略。
- 重复注册拒绝。
- 显式替换。
- 缺失策略失败。
- 名称与快照稳定排序。
- 并发读取和替换。

## 四、配置测试

- 顶层开关默认值。
- 一层和多层继承。
- 缺失列表继承父策略。
- 空列表清空父策略。
- 非空列表整体替换。
- 缺失父策略。
- 直接和间接循环继承。
- `max-cause-depth` 绑定。
- 异常类不存在或不是 Throwable。
- 自定义基础设施 Bean 覆盖。
- 关闭 Spring 事件。
- 关闭 Micrometer 指标。

## 五、集成测试

### HTTP

使用 WireMock 或 MockWebServer 模拟：

- 429 + Retry-After；
- 502/503/504；
- 连接建立失败；
- 读取超时；
- 成功响应但业务状态 PROCESSING。

### 数据库

使用真实数据库制造：

- 死锁；
- 锁等待超时；
- 乐观锁冲突；
- 提交结果不确定。

验证重试边界位于完整事务之外。

### 消息

验证：

- Provider 内部与外层重试总次数；
- 未知发送结果；
- 重试耗尽后 Outbox 或 DLQ 路径；
- 消息 ID 与 retryId/幂等键关系。

### 指标

检查：

- active 最终归零；
- attempts、scheduled、execution 数量正确；
- backoff duration 正确；
- 标签不包含高基数业务 ID。

## 六、当前版本实际验证

当前环境已完成：

- Java 21 主源码 `-Xlint:all -Werror` 编译。
- API/Core 测试源码静态编译。
- 独立核心冒烟测试。
- 48 个 Java 文件注释风格检查。

当前环境没有 Maven，因此真实 Spring Boot 上下文和完整 JUnit 测试仍需在正式仓库执行：

```bash
mvn clean verify
```
