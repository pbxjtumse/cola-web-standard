我认为你这个顺序是对的：**先把事务模板做清楚，再回到 JDBC 幂等组件。**

因为我们现在幂等组件真正卡住的，不是“数据库怎么 update”，而是几个事务边界一直搅在一起：

* `PROCESSING` 到底什么时候提交、什么时候对其他线程可见；
* 业务操作和幂等记录是否必须一个事务；
* 抢占状态为什么可以是一个极短事务；
* 业务成功、`SUCCESS` 状态更新失败怎么办；
* 扫描恢复为什么会存在；
* 重试到底是重试 SQL，还是重新开启一个完整事务。

你之前重试组件的设计其实已经定了一个很重要的原则：**Retry 在外，Transaction 在内，每一次重试重新执行完整事务单元。**

所以我建议事务模板独立成为我们的一个基础技术组件，而且明确定位成：

> **统一管理“单个本地事务边界”的执行、传播、回滚、提交、生命周期与观测的基础组件。**

它不是分布式事务组件，也不是幂等组件，更不是重试组件。

---

# 一、先确定事务模板到底解决什么

我认为我们目前需要的事务模板，不应该只是把 Spring 的：

```java
transactionTemplate.execute(...)
```

重新包一层。

如果只是这样，没有太大意义。

我们真正需要解决的是：

```text
业务代码
   │
   ▼
TransactionExecutor
   │
   ├── 事务定义
   ├── 事务传播
   ├── 隔离级别
   ├── 超时
   ├── rollback 规则
   ├── 生命周期
   ├── afterCommit
   ├── 状态与异常阶段
   ├── 指标 / Trace
   └── Provider
          │
          └── Spring PlatformTransactionManager
                     │
                     └── JDBC / MyBatis / JPA
```

换句话说：

> Spring 负责“怎么开启和提交数据库事务”，我们的组件负责“以什么统一语义使用事务”。

这个边界非常重要。

我们**绝对不要自己重新实现事务管理器**。

---

# 二、事务模板最重要的职责边界

我建议先把下面几个原则定死。

## 1. 只解决本地事务

一期只解决：

```text
一个 TransactionManager
        ↓
一个本地数据库事务
```

例如：

```text
MySQL
PostgreSQL
JDBC
MyBatis
JPA
```

不解决：

```text
DB A + DB B 原子提交

DB + Kafka 原子提交

Service A + Service B 原子提交
```

这些不是本地事务模板应该承担的。

以后涉及：

```text
DB + MQ
```

优先考虑：

```text
Local Transaction
    ├── 业务表
    └── Outbox 表

             ↓

Outbox Publisher

             ↓

MQ
```

而不是把事务模板逐渐做成 XA / Seata。

---

# 三、为什么它对当前幂等组件非常重要

我们现在的 JDBC 幂等其实存在两种完全不同的事务模型。

这是事务模板做完以后，我们必须回头重新认真比较的地方。

---

## 模型 A：业务和幂等记录一个事务

例如：

```text
BEGIN
  │
  ├─ INSERT idempotent_record
  │      status = PROCESSING
  │
  ├─ updateOrder()
  ├─ updateAccount()
  │
  └─ UPDATE idempotent_record
         status = SUCCESS
COMMIT
```

特点是：

> 要么全部成功，要么全部回滚。

如果：

```text
updateOrder() 成功
```

但是：

```text
UPDATE idempotent_record SET SUCCESS
```

失败了：

```text
整个事务 rollback
```

因此不存在：

```text
业务已经成功
但是幂等记录还是 PROCESSING
```

这种中间状态。

这是非常强的一种模式。

但代价也很明显：

> `PROCESSING` 在事务提交以前，对其他事务通常并不是一个已经稳定提交的状态。

而且如果事务里面还有：

```text
HTTP
MQ
RPC
复杂计算
```

那事务可能持续非常久。

这就是我们之前一直讨论“锁是不是要包整个业务”的根源之一。

---

# 四、模型 B：PROCESSING 是一个独立短事务

另外一种就是我们现在逐渐走向的：

```text
Tx-1
────────────────────
抢占幂等记录
INIT → PROCESSING
COMMIT
────────────────────

          ↓

执行业务

          ↓

Tx-2
────────────────────
PROCESSING → SUCCESS
COMMIT
────────────────────
```

此时 `Tx-1` 极短：

```java
transaction.execute(() -> {
    claimProcessing();
});
```

提交以后立刻释放数据库资源。

其他请求马上能看到：

```text
PROCESSING
```

这时候你之前问：

> JDBC 的锁是不是执行完状态更新后就可以释放？

答案就非常清晰了：

**是。**

因为数据库锁保护的是：

```text
谁获得 PROCESSING 所有权
```

而不是保护完整业务执行。

---

但是这种模式马上产生一个新问题：

```text
Tx-1：PROCESSING 成功
        ↓
Business 成功
        ↓
进程宕机
        ↓
Tx-2：SUCCESS 没执行
```

数据库变成：

```text
业务：已经成功
幂等记录：PROCESSING
```

所以我们才需要：

```text
PROCESSING_TIMEOUT
扫描
恢复
对账
再次确认业务状态
```

这也解释了：

> 为什么“可靠幂等 + PROCESSING + 扫描器”会比单事务幂等复杂很多。

事务模板写完以后，这两种模型你会一下子看得非常清楚。

---

# 五、所以事务组件第一原则：它不替业务决定事务边界

事务模板只能提供：

```java
transactionExecutor.execute(...);
```

至于你写：

```java
transactionExecutor.execute(() -> {
    claim();
    business();
    markSuccess();
});
```

还是：

```java
transactionExecutor.execute(() -> claim());

business();

transactionExecutor.execute(() -> markSuccess());
```

是**幂等组件策略决定的**。

事务组件不能偷偷替调用方决定。

---

# 六、我建议我们的事务组件分四期

结合我们现在所有技术组件的风格，我建议还是延续：

```text
L1 → L2 → L3 → L4
```

但这里我会把它理解成能力成熟度，而不是机械地“每一期都一定要实现”。

整体路线：

| 阶段        | 定位       | 核心解决问题                           |
| --------- | -------- | -------------------------------- |
| **一期 L1** | 本地事务核心闭环 | 把当前幂等组件需要的事务能力做好                 |
| **二期 L2** | 事务治理     | 生命周期、配置、观测、多事务管理器                |
| **三期 L3** | 可靠性组件组合  | Retry / Idempotency / Outbox 等组合 |
| **四期 L4** | 高级事务能力   | Savepoint、Reactive、扩展 Provider 等 |

**我们现在只应该实现一期。**

二三四期先设计接口边界，不要全部编码。

---

# 七、一期 L1：本地事务核心闭环

这是我认为你现在应该写的版本。

目标非常明确：

> **能够安全、明确地执行一个完整本地事务单元。**

建议核心 API 不超过 8～10 个模型。

---

## 7.1 核心入口：`TransactionExecutor`

我反而不建议直接叫：

```java
TransactionTemplate
```

因为 Spring 已经有：

```java
org.springframework.transaction.support.TransactionTemplate
```

以后代码里面很容易两个 `TransactionTemplate` 打架。

我建议：

```java
public interface TransactionExecutor {

    <T> T execute(
            TransactionDefinition definition,
            TransactionCallback<T> callback
    );

    void executeWithoutResult(
            TransactionDefinition definition,
            TransactionRunnable callback
    );
}
```

默认实现：

```java
DefaultTransactionExecutor
```

我们整个组件概念仍然可以叫：

> 事务模板组件

只是 Java API 使用：

```text
TransactionExecutor
```

会更清晰。

---

# 八、`TransactionDefinition`

这是事务的不可变配置快照。

一期建议：

```java
public final class TransactionDefinition {

    /**
     * 事务名称。
     */
    private final String name;

    /**
     * 传播行为。
     */
    private final TransactionPropagation propagation;

    /**
     * 隔离级别。
     */
    private final TransactionIsolation isolation;

    /**
     * 超时时间。
     */
    private final Duration timeout;

    /**
     * 是否只读。
     */
    private final boolean readOnly;
}
```

一期其实就够了。

---

# 九、传播行为一期不要全做

Spring 有：

```text
REQUIRED
REQUIRES_NEW
SUPPORTS
MANDATORY
NOT_SUPPORTED
NEVER
NESTED
```

我们第一版没必要一上来全部铺开。

我认为一期最重要的是：

```text
REQUIRED
REQUIRES_NEW
MANDATORY
```

### REQUIRED

有事务：

```text
加入已有事务
```

没有：

```text
创建事务
```

普通业务默认使用。

---

### REQUIRES_NEW

无论外面有没有事务：

```text
挂起已有事务
创建一个全新的事务
```

这个对我们当前幂等组件特别重要。

例如：

```text
claim PROCESSING
```

很可能就是：

```java
REQUIRES_NEW
```

它的目的就是：

> PROCESSING 必须独立提交，不能等外部大事务完成。

---

### MANDATORY

要求：

```text
必须已经处于事务内
```

否则直接报错。

对于某些内部方法特别有价值，比如：

```java
repository.updateBusinessAndIdempotencyState()
```

我们明确要求：

> 这个方法只能在事务中调用。

---

# 十、这里有一个非常容易踩坑的点：加入已有事务 ≠ 已经提交

假设：

```java
outerTransaction(() -> {

    transactionExecutor.execute(REQUIRED, () -> {
        updateOrder();
    });

    sendMessage();
});
```

内部：

```text
TransactionExecutor
```

只是加入了外层事务。

所以：

```text
execute() 返回
```

不代表：

```text
updateOrder 已经 COMMIT
```

真正提交发生在：

```text
outerTransaction
```

结束的时候。

这件事我们的事务组件必须明确知道。

所以我甚至建议内部有：

```java
TransactionParticipation
```

例如：

```text
OWNER
PARTICIPANT
```

含义是：

```text
OWNER
当前 TransactionExecutor 自己创建事务
自己负责 commit / rollback

PARTICIPANT
当前 TransactionExecutor 加入外部事务
最终 commit 由外层决定
```

这个设计后面做幂等会非常有价值。

---

# 十一、一期必须有明确的 TransactionStage

参考我们分布式锁组件的经验，我建议事务组件也不要只扔一个：

```text
TransactionException
```

出来。

至少应该知道失败在哪个阶段。

例如：

```java
public enum TransactionStage {

    VALIDATE,

    RESOLVE,

    BEGIN,

    EXECUTE,

    COMMIT,

    ROLLBACK,

    COMPLETION
}
```

这样以后遇到：

```text
Transaction failed
```

我们可以知道究竟是：

```text
BEGIN 失败

EXECUTE 业务失败

COMMIT 失败

ROLLBACK 失败
```

这几种语义完全不同。

---

# 十二、尤其需要重视 COMMIT 失败

这是事务组件里一个很重要但普通业务代码很少认真考虑的问题。

例如：

```text
客户端
    ↓
COMMIT
    ↓
数据库实际上已经提交
    ↓
网络连接中断
    ↓
客户端收到 SQLException
```

这时候：

```text
commit() 抛异常
```

并不一定等于：

```text
数据库一定没有提交
```

有可能是：

> **提交结果未知。**

所以我们以后不能简单设计：

```text
COMMIT_FAILED = 一定失败
```

建议内部结果至少区分：

```java
public enum TransactionOutcome {

    COMMITTED,

    ROLLED_BACK,

    PARTICIPATED,

    ROLLBACK_ONLY,

    COMMIT_UNKNOWN,

    FAILED
}
```

这里：

```text
COMMIT_UNKNOWN
```

对于：

* 幂等
* 支付
* 消息 Outbox
* 重试

都非常关键。

因为看到：

```text
COMMIT_UNKNOWN
```

以后，**不能直接无脑 Retry**。

应该先：

```text
查询最终业务状态 / 幂等记录
```

这和我们之前讲的：

> “结果未知 ≠ 失败”

是完全一样的可靠性思想。

---

# 十三、Rollback 策略

我建议我们自己的显式事务模板默认采用一个比 `@Transactional` 更容易理解的规则：

> callback 抛出任何 Throwable，事务默认回滚，然后继续向上抛出原异常。

例如：

```java
transactionExecutor.execute(definition, ctx -> {

    updateOrder();

    if (...) {
        throw new BusinessException();
    }

    return order;
});
```

流程：

```text
BusinessException
        ↓
ROLLBACK
        ↓
重新抛出 BusinessException
```

**不要吞异常。**

也不要：

```java
catch (Exception e) {
    return null;
}
```

否则调用方会根本不知道事务失败。

---

# 十四、Rollback 本身也可能失败

比如：

```text
业务异常 A
   ↓
开始 rollback
   ↓
rollback 又 SQLException B
```

这时候不能把：

```text
A
```

丢掉只抛：

```text
B
```

我建议：

```text
A = primary exception
B = suppressed exception
```

即保留真正导致事务回滚的原始业务异常。

这是一期就应该定好的异常语义。

---

# 十五、`TransactionContext`

我建议一期就存在，但保持非常轻。

例如：

```java
public interface TransactionContext {

    /**
     * 本次事务模板执行 ID。
     *
     * 注意：
     * 这不是数据库真正的 transaction id。
     */
    String executionId();

    String transactionName();

    boolean isNewTransaction();

    boolean isRollbackOnly();

    void setRollbackOnly();
}
```

我特别强调：

不要把：

```text
executionId
```

叫：

```text
transactionId
```

否则用户很容易误以为它是：

```text
MySQL trx_id
```

其实只是我们组件的一次执行标识。

---

# 十六、一期要不要 afterCommit？

我的建议是：

## API 先支持，但语义要非常严格

例如：

```java
context.afterCommit(() -> {
    ...
});
```

或者：

```java
TransactionSynchronization
```

后面可以支持：

```text
afterCommit
afterRollback
afterCompletion
```

但是必须牢记：

> `afterCommit()` 不是可靠消息机制。

例如：

```text
DB COMMIT 成功
     ↓
afterCommit()
     ↓
发送 Kafka
     ↓
进程突然宕机
```

消息照样会丢。

因此：

```java
afterCommit(() -> sendMessage())
```

只能用于：

* 清理本地缓存；
* best-effort 通知；
* 非关键动作。

如果要求：

> DB 成功以后消息绝对不能丢

就必须：

```text
业务表
+
Outbox 表
```

放在**同一个本地事务**。

这个边界现在就值得定下来。

---

# 十七、一期最重要的内部执行流程

我建议主流程最后基本就是这样：

```text
execute()
   │
   ▼
VALIDATE
   │
   ▼
Resolve TransactionProvider
   │
   ▼
当前是否已有事务？
   │
   ├───────────────┐
   │               │
   ▼               ▼
创建新事务        加入已有事务
OWNER             PARTICIPANT
   │               │
   └───────┬───────┘
           │
           ▼
        EXECUTE
           │
    ┌──────┴───────┐
    │              │
 success         exception
    │              │
    ▼              ▼
rollbackOnly?    ROLLBACK
    │              │
 ┌──┴──┐           ▼
 │     │          throw
Yes    No
 │     │
 ▼     ▼
ROLLBACK COMMIT
 │       │
 ▼       ▼
return/  COMMITTED
throw
```

但如果是：

```text
PARTICIPANT
```

就不能声称：

```text
COMMITTED
```

应该只是：

```text
PARTICIPATED
```

最终事务结果由外层控制。

这是我认为我们的模板相比普通封装真正应该做好的地方。

---

# 十八、一期模块结构

按照我们目前整个技术组件体系，我建议：

```text
transaction-component
│
├── transaction-api
│
├── transaction-core
│
├── transaction-spi
│
├── transaction-provider-spring
│
├── transaction-starter
│
└── transaction-demo
```

### transaction-api

```text
TransactionExecutor
TransactionCallback
TransactionRunnable

TransactionDefinition
TransactionPropagation
TransactionIsolation

TransactionContext
TransactionStage
TransactionOutcome

TransactionException
```

---

### transaction-spi

核心：

```java
public interface TransactionProvider {
    ...
}
```

它负责抽象：

```text
begin
commit
rollback
suspend/resume
status
```

但是不要自己发明数据库事务协议。

---

### transaction-provider-spring

负责把我们的：

```text
TransactionDefinition
```

映射为 Spring：

```text
PlatformTransactionManager
DefaultTransactionDefinition
TransactionStatus
```

也就是说：

```text
我们定义语义
       ↓
Spring Provider 适配
       ↓
PlatformTransactionManager
       ↓
DataSourceTransactionManager
```

---

### transaction-core

真正实现：

```text
DefaultTransactionExecutor

TransactionDefinitionValidator

TransactionLifecycle

TransactionEventPublisher
```

第一版不要拆几十个小类。

---

### transaction-starter

做：

```text
AutoConfiguration
Properties
Bean
```

---

# 十九、一期暂时不要做什么

第一版我建议明确砍掉：

```text
@Transactional 自定义注解
AOP
动态事务表达式
分布式事务
XA
Saga
TCC
数据库事务日志
事务任务扫描器
事务补偿
Reactive Transaction
复杂 Nested Savepoint
动态 Nacos 配置
事务管理后台
```

因为我们目前真正缺的是：

> **一个可靠而清晰的本地事务执行边界。**

不是做另一个 Spring Transaction Framework。

---

# 二十、二期 L2：事务治理能力

当一期跑稳以后，再做事务治理。

这期的定位是：

> 从“能正确执行事务”，变成“能统一管理事务”。

主要增加：

```text
Named Transaction Policy
TransactionPolicyRegistry

完整传播行为
完整隔离级别

afterCommit
afterRollback
afterCompletion

多 TransactionManager

慢事务监控
事务指标
Trace
Event

事务配置校验
```

例如未来：

```yaml
iron:
  transaction:
    policies:

      idempotency-claim:
        propagation: requires-new
        timeout: 2s

      idempotency-business:
        propagation: required
        timeout: 10s

      batch-write:
        propagation: required
        timeout: 30s
```

业务：

```java
transactionExecutor.execute(
        "idempotency-claim",
        ctx -> claim()
);
```

这样以后不是几十处代码都写：

```java
REQUIRES_NEW
2 seconds
READ_COMMITTED
```

而是形成统一治理。

---

# 二十一、二期还应该解决多数据源

以后肯定会遇到：

```text
order-db
account-db
idempotency-db
```

事务模板可以支持：

```text
managerName
```

比如：

```java
TransactionDefinition.builder()
        .manager("orderTransactionManager")
        .build();
```

但是这里有一个必须定死的规则：

> **RouteKey 必须在事务开始之前确定。**

因为 Spring/JDBC 事务开始以后：

```text
Connection
```

通常已经绑定在线程上了。

例如：

```text
transaction begin
    ↓
routeKey = DB01
    ↓
Connection(DB01)
```

这时候你在事务内部：

```java
routeKey = DB02;
```

通常不会真的切换到 DB02。

所以以后：

```text
Routing
    ↓
Transaction
    ↓
SQL
```

而不是：

```text
Transaction
    ↓
中途 Routing
```

这个原则和我们以后分库分表、幂等 RouteKey 都有直接关系。

---

# 二十二、三期 L3：和其他可靠性组件真正组合

这一期会变得非常有意思。

因为到这里：

```text
Transaction
Retry
Idempotency
Message
Outbox
```

开始真正串起来。

---

## Retry + Transaction

最终推荐：

```java
retryExecutor.execute(() ->
        transactionExecutor.execute(() -> {
            updateOrder();
            updateAccount();
        })
);
```

就是：

```text
Retry
  ↓
Tx-1
失败 rollback
  ↓
Retry
  ↓
重新开启 Tx-2
```

而不是：

```text
Tx
 ↓
Retry SQL
 ↓
Retry SQL
 ↓
Retry SQL
```

尤其数据库死锁：

```text
事务已经被数据库判死
```

这时继续在原事务里面重试 SQL 基本没有意义。

必须：

```text
rollback old transaction
        ↓
new transaction
        ↓
retry
```

---

# 二十三、这里还必须防一个非常隐蔽的问题

假如：

```text
外层已经有 Tx-A
```

然后：

```java
retryExecutor.execute(() ->
    transactionExecutor.execute(REQUIRED, ...)
);
```

每次：

```text
REQUIRED
```

都会加入同一个：

```text
Tx-A
```

那根本不是真正意义上的：

```text
Retry whole transaction
```

因为旧事务根本没结束。

所以三期应该增加一个非常有价值的保护：

```text
transaction retry boundary validation
```

例如：

> Retry Transaction 要求当前不存在外层事务，或者明确使用 REQUIRES_NEW。

否则直接警告甚至拒绝。

这是以后我们的 Retry + Transaction Integration 很值得做的能力。

---

# 二十四、Transaction + Idempotency

这时候幂等组件可以非常清楚地表达两套策略。

### Atomic 模式

```text
Transaction
│
├── claim
├── business
└── success
```

---

### Durable Processing 模式

```text
Transaction(REQUIRES_NEW)
│
└── claim PROCESSING
        │
        ▼
      COMMIT

Business Transaction
        │
        ▼

Transaction(REQUIRES_NEW)
│
└── mark SUCCESS
```

然后配合：

```text
PROCESSING_TIMEOUT
Scanner
Claim
Recovery
```

这时候我们再重新回来做幂等，你之前的很多疑惑都会自然消失。

---

# 二十五、Transaction + Outbox

可靠消息最终应该：

```text
Transaction
│
├── UPDATE order
│
└── INSERT outbox_event
│
└── COMMIT
```

然后：

```text
Outbox Scanner
       ↓
Message Component
       ↓
Retry Component
       ↓
Kafka / Pulsar / RocketMQ
```

这会把我们现在几个组件真正串起来：

```text
Transaction
    ↓
Outbox
    ↓
Retry
    ↓
Message
    ↓
Idempotency Consumer
```

这才是后面的可靠性体系。

---

# 二十六、四期 L4：高级能力

这一期我反而建议放得很远。

可以包括：

```text
NESTED transaction
Savepoint

Reactive Transaction

更多 Provider

事务能力 Capability 描述

事务诊断

更强的事件与指标

慢事务治理

事务使用规范检查
```

例如：

```text
NESTED
```

以后可能实现：

```text
Tx
│
├── update A
│
├── SAVEPOINT
│
├── update B
│
│      X
│
├── rollback to SAVEPOINT
│
└── update C
```

但这和我们当前幂等问题没有关系。

第一版完全没必要碰。

---

# 二十七、四期也不要变成“分布式事务组件”

这个边界我建议长期保持。

我们的：

```text
transaction-component
```

应该一直定义为：

> Local Transaction Component

如果以后真的需要：

```text
Seata
XA
TCC
Saga
```

应该是：

```text
distributed-transaction-component
```

甚至 Saga 更适合：

```text
workflow / orchestration
```

不要把这些东西逐渐塞进：

```text
TransactionExecutor
```

不然最后一定失控。

---

# 二十八、最终我建议我们按这个路线执行

```text
                     Transaction Component
                              │
       ┌──────────────────────┼──────────────────────┐
       │                      │                      │
       ▼                      ▼                      ▼
   L1 一期                 L2 二期                L3 三期
 本地事务闭环               事务治理               可靠性集成
       │                      │                      │
       │                      │                      ├─ Retry
       │                      │                      ├─ Idempotency
       │                      │                      ├─ Outbox
       │                      │                      └─ Message
       │                      │
       │                      ├─ Policy
       │                      ├─ Observability
       │                      ├─ Hooks
       │                      └─ Multi-TM
       │
       ├─ execute
       ├─ REQUIRED
       ├─ REQUIRES_NEW
       ├─ rollback
       ├─ timeout
       ├─ isolation
       ├─ readOnly
       ├─ stage
       └─ Spring Provider

                              │
                              ▼
                           L4 四期
                          高级能力
                              │
                              ├─ NESTED
                              ├─ Savepoint
                              ├─ Reactive
                              └─ Advanced Provider
```

---

# 二十九、结合我们现在的进度，我建议只干这一小块

**当前事务组件一期完成标准，我会定成：**

```text
TransactionExecutor

TransactionDefinition

TransactionCallback

TransactionContext

TransactionPropagation
    REQUIRED
    REQUIRES_NEW
    MANDATORY

TransactionIsolation

TransactionStage

TransactionOutcome

TransactionProvider SPI

SpringTransactionProvider

DefaultTransactionExecutor

事务异常体系

基础 Event

基础 Metrics

Starter

Demo

完整单测
```

然后**立刻停手**。

不要继续做二期。

接着回到：

```text
idempotency-component
```

我们重新把：

```text
claim
PROCESSING
business
SUCCESS
FAILED
PROCESSING_TIMEOUT
scanner
```

全部用明确的：

```text
Tx-1
Tx-2
Tx-3
```

画出来。

到了那一步，之前关于：

> 为什么 claim 能是极短临界区？

> 为什么 PROCESSING 有时候其他线程看不到？

> 为什么有的模式需要 scanner，而有的完全不需要？

> 为什么业务成功但 SUCCESS 写失败是一个特殊危险窗口？

> 为什么 Retry 必须重新开启事务？

这些问题会真正连成一个完整体系。

**所以我建议下一步不是直接写全部事务组件代码，而是先把“一期事务组件的 API、状态模型、模块结构 + 一张完整主时序图”确定下来。确定以后再一次性生成一期代码。**



我建议**不要把分布式事务塞进现在这个 `transaction-component` 里面**。

更准确地说，应该分成两个层级：

```text
transaction-component
    └── 本地事务能力

distributed-transaction-component
    └── 跨资源 / 跨服务事务协调能力
```

而且现阶段我甚至建议：**先不实现 distributed-transaction-component，只把位置和边界预留出来。**

原因是两者解决的问题本质不同。

本地事务解决的是：

```text
BEGIN
  update order
  update account
  insert idempotency
COMMIT
```

核心依赖一个本地 `TransactionManager`，关注：

```text
传播行为
隔离级别
commit / rollback
timeout
rollbackOnly
afterCommit
事务生命周期
```

而分布式事务解决的是：

```text
Service A / DB-A
        +
Service B / DB-B
        +
Service C / MQ
```

核心问题已经变成：

```text
事务协调
参与者
分支事务
全局事务ID
补偿
悬挂
空回滚
幂等
超时恢复
状态机
日志
调度
重试
```

这已经完全不是同一个复杂度了。

---

## 我建议你的工程最终这样划

比如整个技术组件体系：

```text
components
│
├── foundation-component
│
├── concurrency-component
│
├── distributed-lock-component
│
├── retry-component
│
├── transaction-component
│
├── idempotency-component
│
├── message-component
│
├── outbox-component
│
├── task-component
│
└── distributed-transaction-component   ← 后面真的需要再建设
```

也就是说，**它们是兄弟组件，不是：**

```text
transaction-component
├── transaction-local
└── transaction-distributed
```

我不推荐这种结构。

因为这会给人一种错误认知：

> 分布式事务只是本地事务的一个 Provider。

实际上不是。

---

# 一个很重要的边界

我们现在的 `transaction-component` 最好长期坚持：

> **Local Transaction Abstraction**

即使以后有：

```text
transaction-provider-spring
transaction-provider-jdbc
transaction-provider-reactive
```

它们解决的仍然是：

> 一个事务资源管理器之下的事务。

而未来的：

```text
distributed-transaction-component
```

可能采用：

```text
Saga
TCC
Seata AT
XA
可靠事件最终一致性
```

这些东西的编程模型甚至都不一样。

比如本地事务是：

```java
transactionExecutor.execute(() -> {
    updateOrder();
    updateAccount();
});
```

而 TCC 实际上更接近：

```text
TRY
 │
 ├─ Account.tryFreeze()
 └─ Inventory.tryReserve()

          ↓

      全部成功？

     /         \
   Yes         No
    │           │
CONFIRM       CANCEL
```

Saga 又变成：

```text
CreateOrder
    ↓
FreezeBalance
    ↓
ReserveStock
    ↓
CreateDelivery
```

失败：

```text
CreateDelivery X
        ↓
ReleaseStock
        ↓
UnfreezeBalance
        ↓
CancelOrder
```

你看，这已经不是：

```text
commit()
rollback()
```

这种抽象了。

所以不能硬塞进 `TransactionExecutor`。

---

# 那分布式事务组件能不能依赖本地事务组件？

**可以，而且我认为应该。**

依赖方向可以是：

```text
distributed-transaction-component
               │
               ▼
       transaction-api
```

例如一个 Saga Step：

```java
saga.step("freeze-balance", () ->
        transactionExecutor.execute(() -> {
            freezeBalance();
            insertSagaLog();
        })
);
```

或者 TCC 的 Try 阶段：

```java
tryAction(() ->
        transactionExecutor.execute(() -> {
            freeze();
            insertBranchRecord();
        })
);
```

也就是说：

> 分布式事务协调一个个本地事务。

这是正确关系。

而不是：

```text
transaction-component
        ↓
偷偷实现 Saga / TCC
```

---

# 甚至可以把层次理解成这样

```text
                    ┌──────────────────────────┐
                    │ Distributed Transaction  │
                    │ Saga / TCC / Seata ...   │
                    └────────────┬─────────────┘
                                 │
                 协调多个本地事务 / 业务动作
                                 │
              ┌──────────────────┼──────────────────┐
              ▼                  ▼                  ▼

       Transaction           Transaction          Transaction
        Service A             Service B            Service C
           │                     │                    │
          DB-A                  DB-B                 DB-C
```

所以：

> **Local Transaction 是基础能力；Distributed Transaction 是编排与协调能力。**

---

# 但还有一个更关键的问题：我们到底要不要自己做分布式事务？

我的建议是：

## **现在不要。**

至少在我们当前技术组件建设阶段，不值得立刻进入这个坑。

我们目前更值得优先做的是：

```text
Transaction
     ↓
Idempotency
     ↓
Retry
     ↓
Message
     ↓
Outbox
     ↓
Task / Scanner / Claim
```

把这些做好以后，我们其实已经能解决大量所谓的“分布式事务问题”。

比如：

## DB + MQ

我不会优先推荐：

```text
XA
```

而是：

```text
Local Transaction
│
├── update business
└── insert outbox
       │
     COMMIT
       │
       ▼
Outbox Scanner
       │
       ▼
Message Component
```

这是：

> 最终一致性。

---

## 服务 A → 服务 B

如果是：

```text
创建订单
    ↓
通知积分
```

积分失败并不要求：

```text
订单立即 rollback
```

那么：

```text
MQ + Retry + Idempotency
```

就解决了。

完全没有必要上：

```text
TCC
```

---

# 什么时候我们才真正需要 Distributed Transaction？

我认为至少满足这种业务语义：

```text
A 成功
B 成功
C 失败
```

业务明确要求：

> A、B 必须撤销，整个跨服务业务过程必须形成明确的一致性闭环。

而且：

```text
MQ 最终一致性
+
Outbox
+
Retry
+
Idempotency
+
人工修复
```

仍然无法满足业务要求。

这时候才认真考虑：

```text
Saga
TCC
Seata
```

---

# XA 我会放得更后

如果以后我们的分布式事务组件真的建设，我也不会一期直接自己搞 XA。

我的优先级大概会是：

```text
L1
可靠事件 / Saga 基础编排

        ↓

L2
Saga 状态机
补偿
恢复
幂等

        ↓

L3
TCC

        ↓

L4
Seata Provider / 外部框架集成

        ↓

XA
如果真的有非常明确需求再说
```

因为微服务下，XA 的：

```text
资源锁持有
协调器依赖
性能
数据库支持
故障恢复
```

都比较重。

---

# 我甚至建议未来不要叫一个大而全的 `DistributedTransactionExecutor`

这很容易再次抽象过头。

未来如果真做，可以是：

```text
distributed-transaction-component
│
├── distributed-transaction-api
│
├── distributed-transaction-core
│
│
├── distributed-transaction-saga
│
├── distributed-transaction-tcc
│
│
├── distributed-transaction-provider-seata
│
├── distributed-transaction-starter
│
└── distributed-transaction-demo
```

API 可以有统一的一些基础概念：

```text
GlobalTransactionId
TransactionParticipant
TransactionState
TransactionContext
```

但是：

```text
SagaExecutor
```

和：

```text
TccExecutor
```

最好不要强行做成同一个：

```java
DistributedTransactionExecutor
```

因为两者语义差异很大。

---

# 这样一来，我们整个体系会非常清楚

我现在比较认可的最终可靠性架构是：

```text
                     ┌─────────────────────┐
                     │ Distributed Tx      │
                     │   Saga / TCC        │
                     │   以后再做          │
                     └─────────┬───────────┘
                               │
                               ▼
                     ┌─────────────────────┐
                     │ Transaction         │
                     │ Local Transaction   │
                     └─────────┬───────────┘
                               │
         ┌─────────────────────┼─────────────────────┐
         │                     │                     │
         ▼                     ▼                     ▼
   Idempotency              Outbox                 Retry
         │                     │                     │
         │                     ▼                     │
         │                  Message ◄─────────────────┘
         │                     │
         ▼                     ▼
 Distributed Lock       Task / Scanner / Claim
```

注意这里不是严格 Maven 依赖图，而是能力关系图。

---

## 所以对于我们现在的事务组件，我建议 README 第一段就把边界写死

类似：

> `transaction-component` 提供统一的本地事务执行与治理能力，封装事务边界、传播行为、隔离级别、超时、提交、回滚、生命周期及可观测性。
>
> 本组件不提供 XA、TCC、Saga 等分布式事务协调能力。跨资源与跨服务一致性能力由独立的 `distributed-transaction-component` 承担。

这句话我认为非常重要。

否则半年以后很容易有人看到：

```text
transaction-component
```

就开始往里面加：

```text
Seata
Saga
TCC
```

最后组件边界直接废掉。

---

**所以最终我的建议很明确：**

现在：

```text
transaction-component
    = 本地事务
```

以后如果真的需要：

```text
distributed-transaction-component
    = 独立一级技术组件
```

而不是 `transaction-component` 的一个普通子 module。

并且我们当前路线仍然是：

**Transaction 一期 → 回去完成 JDBC Idempotency → Outbox/可靠消息 → 任务恢复。**

做到这里以后，再判断业务到底有没有真正需要 Saga/TCC 的地方。很多现在看起来像“需要分布式事务”的问题，到那时候其实已经被**本地事务 + Outbox + 幂等 + 重试**解决了。
