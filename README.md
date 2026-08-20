# 技术组件体系建设

> 一个面向 Java 后端工程的基础技术组件体系，围绕可复用、可治理、可观测、可扩展的目标，逐步沉淀消息、缓存、重试、分布式锁、并发执行、幂等、事务增强等通用能力。

---

## 1. 项目简介

本项目是一个基于 Java 17、Spring Boot 3 和 Maven 多模块工程建设的技术组件体系。

它不是一个单一业务系统，而是一套面向后端工程复用的基础能力沉淀，目标是把常见的技术能力从业务代码中抽离出来，形成统一、稳定、可持续演进的组件底座。

当前项目主要围绕以下方向建设：

* 基础能力组件
* 消息组件
* 缓存组件
* 重试组件
* 分布式锁组件
* 并发执行组件
* 幂等组件
* 事务增强组件
* 可观测性组件
* Maven / BOM 工程治理

---

## 2. 为什么建设这套组件

在后端系统中，经常会重复遇到类似问题：

* 每个系统都要自己封装 Redis 缓存；
* 每个系统都要处理消息发送失败、消费失败、死信；
* 每个系统都要写重试逻辑；
* 每个系统都要处理重复请求、重复消费、重复任务；
* 每个系统都要处理分布式锁、锁超时、锁续租；
* 每个系统都要维护线程池、异步任务、超时控制；
* 每个系统都要补日志、指标、Trace 和告警；
* 每个项目都有一堆散落的工具类和公共方法。

如果这些能力都散落在业务代码中，长期会导致：

* 代码重复；
* 实现不统一；
* 监控不可控；
* 故障难定位；
* 组件无法复用；
* 后续维护成本越来越高。

因此，本项目希望将这些共性能力沉淀为统一的技术组件，让业务系统可以更专注于业务本身。

---

## 3. 总体目标

本项目的长期目标是建设一套：

* 可复用
* 可治理
* 可观测
* 可扩展
* 可测试
* 可压测
* 可持续演进

的 Java 技术组件体系。

每个组件都应该具备清晰的职责边界、稳定的 API、可替换的实现、统一的配置方式和可观测能力。

---

## 4. 技术基线

当前项目使用的主要技术基线包括：

```text
Java 17
Spring Boot 3
Maven 多模块
Micrometer
Prometheus
Grafana
Jackson
Apache Commons
COLA 分层架构
```

---

## 5. 工程结构

项目整体采用 COLA 分层架构，并在 `component` 目录下沉淀技术组件。

```text
cola-web-standard
├── adapter
├── app
├── client
├── domain
├── infrastructure
├── start
└── component
```

其中：

| 目录               | 说明                       |
| ---------------- | ------------------------ |
| `adapter`        | 入站适配层，例如 Controller、外部入口 |
| `app`            | 应用服务层，负责编排业务用例           |
| `client`         | 对外暴露的接口、DTO、Client 模型    |
| `domain`         | 领域模型和领域服务                |
| `infrastructure` | 基础设施实现，例如数据库、外部服务        |
| `start`          | Spring Boot 启动模块         |
| `component`      | 技术组件和基础能力沉淀              |

---

## 6. 当前组件概览

### 6.1 Foundation 基础组件

Foundation 是所有技术组件共同依赖的底层能力。

它提供：

* 字符串、集合、Map、数字等通用工具门面；
* ID 生成能力；
* 时间与 Deadline 能力；
* 上下文模型；
* 编解码能力；
* 序列化能力；
* 资源读取能力；
* 反射辅助能力；
* 测试支持能力。

Foundation 的目标不是重新发明一个庞大的工具库，而是在 JDK 和成熟开源库之上，提供一层统一、轻量、稳定的项目级基础门面。

---

### 6.2 Retry 重试组件

重试组件用于统一处理“某个操作失败后是否需要再次执行”的问题。

它关注：

* 最大重试次数；
* 最大执行时长；
* 单次尝试耗时；
* 异常是否可重试；
* 返回结果是否需要重试；
* 固定退避；
* 指数退避；
* 随机抖动；
* 中断处理；
* 重试事件；
* 后续指标接入。

重试组件只负责“是否再次执行”，不负责业务幂等，也不负责业务补偿。

---

### 6.3 Message 消息组件

消息组件用于统一消息发送、订阅、确认、失败处理和多消息中间件适配。

规划支持：

* 统一消息模型；
* 统一发送 API；
* 统一消费 API；
* Kafka 适配；
* RocketMQ 适配；
* Pulsar 适配；
* 消息 Header 透传；
* 消息失败处理；
* 死信处理；
* 与重试、幂等、可观测能力联动。

---

### 6.4 Cache 缓存组件

缓存组件用于统一缓存访问和多级缓存能力。

规划支持：

* 本地缓存；
* Redis 缓存；
* 二级缓存；
* TTL 管理；
* 缓存预热；
* 缓存失效；
* 防穿透；
* 防击穿；
* 防雪崩；
* 序列化统一；
* 缓存指标。

---

### 6.5 Distributed Lock 分布式锁组件

分布式锁组件用于提供分布式互斥控制能力。

规划支持：

* Redis 分布式锁；
* ownerToken；
* lease；
* 自动续租；
* 等待超时；
* 锁释放保护；
* 锁丢失感知；
* Fencing Token；
* 锁指标。

分布式锁组件不替代事务，也不替代幂等。

---

### 6.6 Concurrency 并发执行组件

并发组件用于统一异步任务、线程池治理和并行执行能力。

规划支持：

* 异步任务执行；
* 并行任务编排；
* 线程池隔离；
* 超时控制；
* fallback；
* 任务取消；
* 上下文传播；
* 队列等待时间统计；
* 线程池指标。

---

### 6.7 Idempotency 幂等组件

幂等组件用于解决重复请求、重复消费、重复任务执行等问题。

规划支持：

* RPC 幂等；
* 消息幂等；
* 任务幂等；
* 幂等 Key；
* 幂等状态机；
* 处理中状态；
* 结果缓存；
* 事务集成；
* 超时恢复。

---

### 6.8 Transaction 事务增强组件

事务增强组件用于提供统一的本地事务模板能力。

规划支持：

* 本地事务模板；
* 事务边界控制；
* 事务提交和回滚观测；
* 与重试组件组合；
* 与幂等组件组合；
* 与 Outbox 组件组合。

---

### 6.9 Observability 可观测性组件

可观测性组件用于统一日志、指标、Trace、上下文和告警接入规范。

当前规划基于：

```text
Micrometer
Prometheus
Grafana
Spring Boot Actuator
```

主要关注：

* 调用次数；
* 成功数量；
* 失败数量；
* 延迟；
* P95 / P99；
* 活跃任务数；
* 队列堆积；
* 重试放大；
* 消息堆积；
* 锁竞争；
* 缓存命中率。

---

## 7. 当前进度

当前项目处于持续建设阶段，主要进展如下：

| 模块               | 当前状态                                 |
| ---------------- | ------------------------------------ |
| Foundation       | 已形成基础设计，正在稳定接口                       |
| Retry            | 核心设计较完整，正在接入 Foundation              |
| Message          | 已有一期基础，后续接入 Foundation Serialization |
| Cache            | 已有基础代码，后续完善多级缓存与指标                   |
| Distributed Lock | 设计较完整，后续接入 Foundation Time / ID      |
| Concurrency      | 已有基础能力，后续接入 Foundation Context       |
| Transaction      | 已有设计雏形，后续与幂等、Outbox 对齐               |
| Idempotency      | 规划中，属于后续重点组件                         |
| Observability    | 指标标准已明确，待各组件逐步接入                     |
| Maven / BOM      | 已开始规范化，持续修正依赖边界                      |

---

## 8. 工程治理

本项目会持续完善 Maven 多模块治理。

当前已经明确：

* 根 POM 负责 Java、Spring Boot、插件和第三方 BOM；
* `component-bom` 负责管理可消费技术组件版本；
* 聚合模块只负责聚合，不作为业务依赖；
* 子模块只声明真实直接依赖；
* Demo 和测试模块不进入生产 BOM；
* Starter 需要控制依赖边界，避免一次性引入所有 Provider。

---

## 9. 可观测性规划

组件指标统一采用如下链路：

```text
组件代码
    ↓
Micrometer
    ↓
Spring Boot Actuator /actuator/prometheus
    ↓
Prometheus
    ↓
Grafana
```

指标命名建议：

```text
iron.<component>.<object>.<metric>
```

示例：

```text
iron.retry.execution.total
iron.retry.attempt.total
iron.message.send.total
iron.message.consume.duration
iron.cache.hit.total
iron.lock.acquire.duration
iron.concurrency.task.timeout.total
```

第一批计划接入指标的组件：

1. Retry
2. Message
3. Cache
4. Distributed Lock
5. Concurrency

---

## 10. 项目当前阶段重点

当前阶段最重要的工作不是继续新增更多组件，而是把已经规划和建设中的核心组件打通。

近期重点包括：

1. 稳定 Foundation 基线；
2. 修复 Maven 构建链路；
3. Retry 接入 Foundation；
4. Message 接入 Foundation Serialization；
5. Cache 接入 Foundation Serialization / Codec；
6. Distributed Lock 接入 Foundation Time / ID；
7. Concurrency 接入 Foundation Context；
8. 补齐单元测试；
9. 补齐集成测试；
10. 接入 Prometheus / Grafana；
11. 建立组件级压测场景。

---

## 11. 后续路线图

### 第一阶段：基础能力稳定

* 固定 Foundation 命名和 API；
* 完成 Maven / BOM 规范；
* 确保项目完整编译；
* 补齐基础单元测试；
* 输出组件 README。

### 第二阶段：核心组件接入

* Retry 接入 Foundation；
* Message 接入 Foundation Serialization；
* Cache 接入 Foundation Serialization；
* Lock 接入 Foundation Time / ID；
* Concurrency 接入 Foundation Context。

### 第三阶段：可观测性接入

* 建立 Metrics Facade；
* 接入 Micrometer；
* 暴露 Prometheus 指标；
* 建立 Grafana Dashboard；
* 补充告警规则。

### 第四阶段：可靠性验证

* 单元测试；
* 集成测试；
* 压测；
* 故障注入；
* 指标验证；
* Demo 验证。

### 第五阶段：业务场景验证

* 接入真实业务场景；
* 验证组件易用性；
* 验证性能表现；
* 验证故障恢复能力；
* 继续收敛 API 与默认配置。

---

## 12. 项目原则

本项目遵循以下原则：

1. 技术组件不承载业务语义；
2. 基础组件只做底座，不反向依赖上层组件；
3. 优先使用 JDK 和成熟开源库；
4. 自研代码保持克制；
5. 组件边界优先于功能堆叠；
6. 所有核心组件必须可观测；
7. Starter 依赖必须可控；
8. API 应稳定，内部实现可以演进；
9. 测试和压测结果比设计文档更重要；
10. 长期维护优先于短期炫技。

---

## 13. 当前状态说明

本项目仍处于持续建设中，并非所有组件都已经达到生产成熟状态。

当前更准确的定位是：

```text
基础技术组件体系建设中
```

适合用于：

* 技术组件设计学习；
* Java 后端架构实践；
* Spring Boot 3 多模块工程实践；
* Maven / BOM 治理实践；
* 消息、缓存、重试、锁、幂等等基础组件沉淀；
* 后续技术中台能力建设。

---

## 14. 目录建议

建议后续文档目录如下：

```text
docs
├── 技术组件建设.md
├── foundation-component.md
├── retry-component.md
├── message-component.md
├── cache-component.md
├── distributed-lock-component.md
├── concurrency-component.md
├── idempotency-component.md
├── transaction-component.md
├── observability-metrics.md
└── maven-bom-standard.md
```

---

## 15. 一句话总结

本项目正在建设一套面向 Java 后端工程的技术组件体系。

它的目标不是简单封装工具类，而是逐步形成一套：

```text
基础能力统一
组件边界清晰
接入方式统一
指标观测完整
可测试可压测
可长期演进
```

的技术底座。
