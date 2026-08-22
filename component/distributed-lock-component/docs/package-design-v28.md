# Distributed Lock V28 分包设计

本版保持 Maven module 结构不变：`api / core / provider / starter / demo`。本次只优化各 module 内部 Java package，不新增 `distributed-lock-spi` module。

## Core

```text
core
├── client          # Client 门面、LockHandle 与运行时状态
├── acquire         # acquire 主流程与 acquire outcome handlers
├── execute         # callback 执行、release outcome、最终结果归并
├── spi             # LockProvider 扩展契约与默认 registry
│   └── protocol    # Provider acquire/check/renew/release 请求、响应、状态、lease
├── fencing         # fencing 决策
│   └── flow        # NONE/NATIVE/EXTERNAL 执行流程
├── wait            # 未获取锁时的等待策略
├── watchdog        # 获取锁后的 lease 生命周期
├── observability   # event + metrics
└── support         # lockName 与 ownerToken 等内部辅助策略
```

原则：优先按一次运行流程阅读，不再为 `request/response/status/runtime/result` 这类技术类型单独建立大量小包。`fencing.flow`、`wait`、`watchdog` 保持独立，因为它们已经是明确且可独立演进的领域职责。

## Starter

```text
starter
├── autoconfigure   # Spring Boot AutoConfiguration
├── properties      # 配置绑定
├── observability   # Spring Event / Micrometer / Health
├── redis            # Spring Data Redis adapter
└── redisson         # RedissonClient 选择与创建
```

Provider Maven 聚合层继续保持 `redis / redisson / jdbc-fencing` 三个独立 jar，不把底层实现混在一起。
