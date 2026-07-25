# v24 Client Flow Balanced Refactor

本次重构没有继续为每一个 builder、校验步骤或 private 方法创建独立类，而是在“职责收敛”和“避免类爆炸”之间做平衡。

## 最终保留的三层流程

```text
DefaultDistributedLockClient
├── LockAcquisitionService
│   ├── 参数解析与校验
│   ├── LockProvider 选择
│   ├── fencing plan 生成
│   ├── LockAcquireRequest 组装
│   ├── LockWaiter 调用
│   └── LockAcquireOutcomeHandlerRegistry 分发
│
└── LockExecutionTemplate
    ├── 获取锁
    ├── watchdog 生命周期
    ├── callback 异常分类
    ├── release
    ├── LockResultResolver
    └── 最终事件与指标
```

## 有意没有拆出的类

以下内容仍然作为流程内的短小方法或 builder 保留：

- `LockAcquireRequestFactory`
- `LockAcquireOutcomeContextFactory`
- `LockAcquirePreparationService`
- `LockCallbackInvoker`
- `LockReleaseCoordinator`
- `LockResultMapper`
- `LockResultFactory`

原因是它们当前没有独立变化轴，单独抽类只会增加代码跳转和构造依赖。

## 合并项

原来的：

```text
LockHandleFactory（接口）
DefaultLockHandleFactory（唯一实现）
```

合并为一个具体的：

```text
LockHandleFactory
```

它仍然有价值，因为它集中隐藏 `DefaultLockHandle` 的运行态、事件和指标依赖；但没有必要为唯一实现保留接口层。

## Client 构造函数

`DefaultDistributedLockClient` 不再保留多组兼容构造函数，只依赖两个稳定流程对象：

```java
new DefaultDistributedLockClient(acquisitionService, executionTemplate)
```

Spring Starter 负责完整装配；测试使用 test fixture 装配，不再反向污染生产 Client 的构造函数。

## 额外修正

`waitDuration` 现在只统计 `LockWaiter.waitForLock(...)` 的实际耗时，不再把参数校验、Provider 选择和事件发布耗时混入等待时间。
