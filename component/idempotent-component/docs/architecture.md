# 分布式幂等架构

## 1. 最终心智模型

不要把幂等理解成“一锁二查三判断”。当前设计是：

```text
稳定识别逻辑请求
      ↓
Repository 原子授予 execution generation
      ↓
StateMachine 决定 EXECUTE / REPLAY / RETURN
      ↓
合法 owner 执行业务
      ↓
owner + version 条件完成最终状态
      ↓
同库时 Transaction 保证 Business + SUCCESS 原子性
      ↓
历史 SUCCESS 只 Replay，不重新执行业务
      ↓
异常 generation 只能由显式 Recovery 二次 CAS 接管
```

## 2. 各层职责

| 能力 | 职责 |
|---|---|
| `IdempotencyRequest` | 这一次请求是谁 |
| `IdempotencyPolicy` | 这类业务怎么做幂等 |
| Repository | 原子判断并转换幂等记录，决定当前 generation owner |
| `ownerToken + version` | 识别当前 generation，拒绝 stale owner |
| StateMachine | 把 Repository 原子结果翻译为 EXECUTE / REPLAY / RETURN |
| DistributedLock | 只减少热点抢占竞争 |
| Transaction Integration | 同库下保护 Business + SUCCESS 原子性 |
| ResultPolicy | SUCCESS 后重复请求返回什么 |
| RecoveryPolicy | 哪些异常 generation 可由外部可靠任务接管 |

## 3. 三层状态模型

持久状态只有：

```text
PROCESSING
SUCCESS
FAILED
```

Repository 判定状态包括：

```text
ACQUIRED
SUCCESS
PROCESSING_ACTIVE
PROCESSING_EXPIRED
FAILED_RETRYABLE
FAILED_FINAL
KEY_CONFLICT
PROVIDER_ERROR
```

Recovery 还包括：

```text
RECOVERY_ACQUIRED
STALE_CANDIDATE
NOT_RECOVERABLE
NOT_FOUND
```

Executor 最终返回的是一次调用结果，如：

```text
EXECUTED
RECOVERED
REPLAYED
PROCESSING
PROCESSING_EXPIRED
PREVIOUS_FAILED_RETRYABLE
PREVIOUS_FAILED_FINAL
KEY_CONFLICT
OWNERSHIP_LOST
TRANSACTION_*
RESULT_POLICY_*
```

不要把数据库状态、Repository 判定和一次 API 调用结果混成同一套 enum。

## 4. execution generation

第一次获得执行权：

```text
status=PROCESSING
ownerToken=A
version=1
```

Recovery 接管：

```text
status=PROCESSING
ownerToken=B
version=2
```

`ownerToken + version` 是当前 execution generation 的身份证。

旧 A 恢复后执行：

```sql
UPDATE ...
WHERE status='PROCESSING'
  AND owner_token='A'
  AND version=1;
```

如果当前已是 B/2，更新 0 行，A 无权完成 SUCCESS 或 FAILED。

## 5. Lock 的位置

正确顺序：

```text
optional lock
    ↓
Repository.tryAcquire / tryRecover
    ↓
unlock
    ↓
StateMachine
    ↓
Business
```

锁不包完整业务。即使锁失效，Repository 的 UNIQUE / Lua / row lock / CAS 仍必须独立保证正确性。

## 6. Transaction 的位置

Transaction 在 StateMachine 已经授予 `EXECUTE` 后参与：

```text
Tx-A: PROCESSING claim
      ↓
StateMachine -> EXECUTE
      ↓
Tx-B: Business + ResultPolicy.capture + SUCCESS
      ↓ failure
Tx-C: FAILED
```

Lock 解决竞争压力，Repository 解决 ownership，Transaction 解决本地原子提交，它们不是同一层。

## 7. WINDOWED 与 DURABLE

`WINDOWED`：同 key 只在有限窗口内属于同一逻辑请求。窗口结束后允许开启新 generation。

`DURABLE`：幂等事实长期有效，不能因为短 TTL 到期就忘记成功事实。

默认映射：

```text
WINDOWED -> Redis
DURABLE  -> JDBC
```

但这是 Starter 默认选择，不是 Core 写死。Repository 通过 capabilities 声明自身支持能力。

## 8. WINDOWED 的三个时间

```text
processingTimeout
idempotencyWindow
recordRetentionTtl
```

- `processingTimeout`：当前 generation 的执行权租约；
- `idempotencyWindow`：相同 key 仍算同一逻辑请求的时间；
- `recordRetentionTtl`：窗口结束后旧物理记录额外保留多久。

例如：

```text
10:00      首次执行
10:00:30   execution lease 到期
10:05      幂等语义窗口结束
11:05      旧记录允许物理清理
```

## 9. 当前兼容项

代码中仍有少量 deprecated 兼容符号，例如 `IdempotencyMode.SHORT_TERM`、旧 ResultCodec 等；新代码和文档统一使用当前推荐 API，不再围绕历史版本展开。
