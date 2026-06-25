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
