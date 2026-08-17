# Code Walkthrough - V1.3

建议按下面顺序阅读。

1. `IdempotencyMode`
   - WINDOWED / DURABLE；
   - SHORT_TERM 只是 deprecated alias。

2. `IdempotencyPolicy`
   - processingTimeout；
   - idempotencyWindow；
   - recordRetentionTtl；
   - RecoveryPolicy；
   - LockOptions。

3. `IdempotencyRequest`
   - key / requestHash / routeKey / policyName；
   - 请求身份与执行策略已经分开。

4. `IdempotencyResultPolicy<T>`
   - NONE / SNAPSHOT / REFERENCE；
   - 理解为什么 Executor 不再需要 `Class<T>`。

5. `IdempotencyRepository` + `IdempotencyRepositoryCapabilities`
   - 正确性核心；
   - Provider 自己声明 WINDOWED/DURABLE/事务/恢复查询能力。

6. `JdbcIdempotencyRepository`
   - UNIQUE；
   - Tx-A；
   - SELECT FOR UPDATE；
   - owner/version CAS；
   - WINDOWED row restart。

7. `RedisIdempotencyRepository` + Lua
   - WINDOWED 原子状态机；
   - semantic window 与 physical retention 分离。

8. `DefaultIdempotencyStateMachine`
   - 只解释 Repository 原子结果；
   - 不参与并发互斥。

9. `DefaultIdempotencyExecutor`
   - Policy resolution；
   - optional short lock；
   - state decision；
   - execute / replay / return；
   - ResultPolicy；
   - Transaction integration。

10. `idempotent-integration-transaction`
    - `SpringTransactionJdbcExecutionManager`；
    - `TransactionTemplateIdempotencyTransactionCoordinator`；
    - Tx-A / Tx-B / Tx-C。

11. `DefaultIdempotencyRecoveryQueryService`
    - policyName 扫描；
    - 扫描与 recover 二次 CAS。

12. `IdempotencyAutoConfiguration`
    - 命名 Policy；
    - Repository registry；
    - transaction-aware JDBC；
    - Jackson SnapshotPolicyFactory。

最重要的阅读主线：

```text
Policy
  ↓
Repository CAS/Lua
  ↓
StateMachine
  ↓
Business
  ↓
ResultPolicy
  ↓
final owner/version CAS
```
