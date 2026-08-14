# Transaction Component L1 设计说明

## 1. 核心原则

1. `transaction-component` 默认表示本地事务。
2. 不重新实现数据库事务协议，Provider 只适配成熟事务管理器。
3. ORM 不是 Transaction Provider 的扩展维度。
4. REQUIRED 的“内部 execute 返回”不等于外层物理事务已提交。
5. REQUIRES_NEW 的 suspend/resume 由底层事务管理器负责。
6. callback 业务异常触发 rollback 后原样抛出。
7. rollback 自身失败时，原始业务异常保持 primary，rollback 基础设施异常作为 suppressed。
8. 普通 commit 基础设施异常按 `COMMIT_UNKNOWN` 保守处理。
9. 事件监听属于观测能力，监听器失败不能影响事务结果。
10. 一期只支持单一默认 `PlatformTransactionManager`。
11. MyBatis Demo 使用 XML Mapper，不使用 `@Insert/@Select` SQL 注解。
12. 自动测试不连接真实 MySQL，真实连接只用于手工运行 Demo。

## 2. 包结构

### transaction-api

```text
com.xjtu.iron.transaction.api
├── context
│   ├── TransactionContext
│   └── TransactionParticipation
├── definition
│   ├── TransactionOptions
│   ├── TransactionPropagation
│   └── TransactionIsolation
├── event
│   ├── TransactionEvent
│   ├── TransactionEventType
│   └── TransactionEventListener
├── exception
│   └── TransactionExecutionException
├── execution
│   ├── TransactionExecutor
│   ├── TransactionCallback
│   └── TransactionRunnable
└── status
    ├── TransactionStage
    └── TransactionOutcome
```

### transaction-spi

```text
spi
├── provider
└── exception
```

### transaction-core

```text
core
├── executor
├── context
└── validation
```

### transaction-provider-spring

```text
provider.spring
├── transaction
├── context
└── mapping
```

## 3. 为什么不提供 MyBatis/JPA Provider

事务组件适配的是事务基础设施，不是 SQL/ORM API。

```text
TransactionExecutor
        ↓
SpringTransactionProvider
        ↓
PlatformTransactionManager
        ↓
 ┌──────┴────────┐
 ↓               ↓
DataSource TM    Jpa TM
 ↓               ↓
MyBatis/JDBC     JPA/Hibernate
```

MyBatis Demo 的 SQL 完全放在：

```text
transaction-demo-mybatis/src/main/resources/mapper/DemoRecordMapper.xml
```

全局 MyBatis XML 配置在：

```text
transaction-demo-mybatis/src/main/resources/mybatis-config.xml
```

事务 XML 不存在，也不应该存在：事务由 Spring TransactionManager 建立，MyBatis Mapper 只参与当前事务。

## 4. 数据库配置

两个 Demo 的主运行配置都指向：

```text
jdbc:mysql://www.xjtu-iron.online:30306/test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8
```

用户名、密码通过环境变量提供：

```text
TX_DEMO_MYSQL_USERNAME
TX_DEMO_MYSQL_PASSWORD
```

测试 profile 使用 H2，防止 `mvn test` 自动写入真实数据库。

## 5. 对幂等组件的直接价值

### Atomic

```text
BEGIN
 claim
 business
 success
COMMIT
```

### Durable Processing

```text
Tx-1(REQUIRES_NEW): claim PROCESSING -> COMMIT
business
Tx-2(REQUIRES_NEW): mark SUCCESS -> COMMIT
```

第二种模式暴露 PROCESSING 中间状态，因此需要 timeout / scanner / recovery；这是事务边界带来的必然复杂度，不应由 Transaction Component 隐藏。

详细逐流程说明见 `flow-v1-detailed.md`。
