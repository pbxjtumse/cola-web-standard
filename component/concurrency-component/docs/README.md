# docs 文档导航

## 本文适合谁看

适合所有想系统阅读并行组件文档的人。

## 读完你会知道什么

- 每份文档的定位。
- 业务开发应该先看什么。
- 组件维护者应该先看什么。
- 排障人员应该先看什么。

## 目录

- [1. 文档分层原则](#1-文档分层原则)
- [2. 文档列表](#2-文档列表)
- [3. 按角色阅读](#3-按角色阅读)
- [4. 文档维护约定](#4-文档维护约定)

## 1. 文档分层原则

并行组件文档按三层组织：

```text
第一层：业务使用者文档
  目标是让业务代码能直接接入。

第二层：组件设计文档
  目标是让维护者理解内部流转、状态机、线程池和错误模型。

第三层：质量与演进文档
  目标是保障长期可维护、可测试、可演进。
```

## 2. 文档列表

| 文档 | 类型 | 说明 |
|---|---|---|
| USER_GUIDE.md | 使用者文档 | 只讲业务怎么用，不讲内部细节 |
| QUICK_START.md | 使用者文档 | 最快接入并跑通第一个任务 |
| API_GUIDE.md | 接口文档 | Public API、Extension API、Internal SPI |
| INTERNAL_DESIGN.md | 设计文档 | 内部组件如何协作 |
| FLOW_AND_STATE.md | 设计文档 | 状态机、时序图、关键流转 |
| THREAD_POOL_AND_REJECTION.md | 专题文档 | 线程池和拒绝策略 |
| TIMEOUT_FALLBACK_CANCEL.md | 专题文档 | 超时、fallback、取消 |
| OBSERVABILITY_AND_ERROR.md | 专题文档 | 错误分类、监听器、指标、注册表 |
| TESTING.md | 质量文档 | 单元测试、并发测试、集成测试 |
| FAQ.md | 问答文档 | 高频问题解释 |
| ROADMAP.md | 规划文档 | 二期、三期和长期演进 |

## 3. 按角色阅读

### 3.1 业务开发

```text
USER_GUIDE.md
→ QUICK_START.md
→ API_GUIDE.md 的 Public API
→ FAQ.md
```

### 3.2 组件维护者

```text
FLOW_AND_STATE.md
→ INTERNAL_DESIGN.md
→ THREAD_POOL_AND_REJECTION.md
→ TIMEOUT_FALLBACK_CANCEL.md
→ TESTING.md
```

### 3.3 排障人员

```text
FLOW_AND_STATE.md
→ OBSERVABILITY_AND_ERROR.md
→ FAQ.md
```

## 4. 文档维护约定

每份长文档都应包含：

```text
本文适合谁看
读完你会知道什么
目录
正文
常见误区或注意事项
```

图形原则：

```text
默认使用 Mermaid，保证 GitHub/GitLab/IDEA Markdown 更容易预览。
复杂 UML 可在 docs/uml 下补充 PlantUML 源文件。
```
## 5 代码计划
### 5.1 一期代码计划
```text
统一异步任务定义方式。
统一异步任务提交入口。
支持普通异步任务、带返回值任务、批量并行任务。
支持任务超时控制。
支持超时后的取消和线程中断。
支持线程池拒绝感知。
支持任务失败后的 fallback。
支持任务执行状态事件通知。
支持错误分类和统一异常包装。
支持基础指标采集和健康检查。
支持线程池注册、查询和基础动态调整。
为后续二期的动态线程池、压测治理、配置中心接入、重试组件接入打基础
```
### 5.2 二期代码计划
```text
1. 动态线程池治理
2. 线程池健康检查和繁忙判断
3. 指标体系增强 
4. 内部诊断能力
5. TaskExecutionRegistry 查询增强
6. 本地取消语义补强
7. timeout / fallback 边界补强
8. fallbackExecutor / timeoutScheduler 生命周期治理
9. 配置体系和校验增强
10. 上下文传播增强
11. AsyncTemplate 批量并行增强
12. 完整 JUnit 5 测试体系
13. Spring Boot 自动装配测试
14. Demo 和文档完善
```
