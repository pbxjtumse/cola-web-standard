对，你这里的理解其实更接近我最终会推荐给你的设计：

> **只要幂等状态承担“业务事实”的职责，我更倾向把它持久化在关系型数据库，而不是只放 Redis。**

我上一条把“普通接口防重复 → Redis Lua”写得有点太宽泛了。更准确地说：

> **Redis + Lua 很适合做短生命周期的“重复请求抑制 / 去重状态”；关系型数据库更适合做需要长期可信、可恢复、可审计的“业务幂等状态”。**

这两个看起来都叫 Idempotency State，但它们承担的责任其实不完全一样。

---

# 1. 先理解：什么叫 `Idempotency State`

假设接口：

```http
POST /orders
```

业务请求：

```json
{
  "requestId": "REQ-10001",
  "userId": "U001",
  "productId": "P001"
}
```

客户端因为网络超时连续调用三次：

```text
REQ-10001
REQ-10001
REQ-10001
```

幂等组件真正需要回答的并不是：

> “现在有没有人拿到锁？”

而是：

> **这个业务请求 REQ-10001 到底执行到什么状态了？**

这就是：

```text
Idempotency State
```

例如：

```text
不存在
    ↓
PROCESSING
    ↓
SUCCESS
```

或者：

```text
PROCESSING
    ↓
FAILED
```

数据库里可以是一条记录：

```text
idempotency_key = REQ-10001
status          = SUCCESS
result          = orderId=ORDER-888
```

以后再来：

```text
REQ-10001
```

幂等组件查到：

```text
SUCCESS
```

就知道：

> 这不是一个“新的请求”，而是已经成功执行过的同一个逻辑请求。

所以直接返回：

```text
ORDER-888
```

而不是再次创建订单。

---

# 2. 所以 Idempotency State 本质上是一条“业务执行记录”

我建议未来定义类似：

```java
public final class IdempotencyRecord {

    /**
     * 幂等唯一键。
     */
    private String idempotencyKey;

    /**
     * 当前执行状态。
     */
    private IdempotencyStatus status;

    /**
     * 本次请求内容摘要。
     */
    private String requestHash;

    /**
     * 当前执行者。
     */
    private String ownerToken;

    /**
     * 状态版本。
     */
    private long version;

    /**
     * 执行成功后的结果。
     */
    private String result;

    /**
     * 失败信息。
     */
    private String errorCode;

    /**
     * PROCESSING 开始时间。
     */
    private Instant startedAt;

    /**
     * PROCESSING 租约过期时间。
     */
    private Instant processingExpireAt;

    /**
     * 最终完成时间。
     */
    private Instant completedAt;

    private Instant createdAt;

    private Instant updatedAt;
}
```

状态：

```java
public enum IdempotencyStatus {

    PROCESSING,

    SUCCESS,

    FAILED
}
```

这就是所谓的：

> **幂等状态机。**

---

# 3. 为什么不能只有一个 `exists=true/false`

很多简单幂等实现会这样：

```text
Redis:

SETNX idempotency:REQ-10001 1
```

存在：

```text
说明执行过了
```

不存在：

```text
说明没执行过
```

这其实不够。

因为“执行过”至少有三种完全不同的情况：

```text
1. 正在执行
2. 已经成功
3. 执行失败
```

所以：

```text
exists
```

不足以描述真实业务。

正确模型应该至少是：

```text
            不存在
               │
               │ 抢占
               ▼
           PROCESSING
          /          \
         /            \
      成功              失败
       ↓                ↓
    SUCCESS           FAILED
```

---

# 4. `PROCESSING` 是整个幂等组件最关键的状态

例如 A 请求来了：

```text
REQ-10001
```

数据库没有记录。

A 原子执行：

```sql
INSERT INTO idempotency_record (
    idempotency_key,
    status,
    owner_token
)
VALUES (
    'REQ-10001',
    'PROCESSING',
    'OWNER-A'
);
```

成功。

那么这代表：

> **A 抢到了 REQ-10001 的执行权。**

此时 B 又来了：

```text
REQ-10001
```

它查询：

```text
PROCESSING
```

B 就不能继续执行业务。

它可以根据策略：

```text
立即返回：
    REQUEST_PROCESSING

或者等待：

    等 A 完成

或者：
    返回 202 Accepted
```

所以这里其实已经不一定需要分布式锁了。

数据库本身：

```text
UNIQUE(idempotency_key)
```

已经实现了一次原子抢占。

---

# 5. `SUCCESS` 又是什么意思？

A 业务完成：

```text
创建订单 ORDER-888
```

然后：

```sql
UPDATE idempotency_record
SET status = 'SUCCESS',
    result = 'ORDER-888',
    completed_at = NOW()
WHERE idempotency_key = 'REQ-10001'
  AND owner_token = 'OWNER-A'
  AND status = 'PROCESSING';
```

之后 B/C/D 再来：

```text
REQ-10001
```

查到：

```text
SUCCESS
result = ORDER-888
```

可以直接：

```text
return ORDER-888
```

这才是真正完整的：

> **重复请求返回第一次执行结果。**

---

# 6. `FAILED` 又为什么需要保存？

假设 A：

```text
PROCESSING
    ↓
调用业务
    ↓
参数业务校验失败
```

可以写：

```text
FAILED
```

但 FAILED 还要区分：

```text
可重新执行
不可重新执行
```

比如：

### 参数错误

```text
amount = -100
```

失败后：

```text
FAILED_PERMANENT
```

没有必要再执行。

### 网络瞬时故障

```text
调用下游 timeout
```

可能：

```text
FAILED_RETRYABLE
```

下一次同幂等 Key 可以允许重新抢占。

因此未来甚至可能演进成：

```text
PROCESSING
SUCCESS
FAILED_RETRYABLE
FAILED_FINAL
```

不过一期我建议先不要把状态爆炸开。

可以：

```text
FAILED + failureType
```

---

# 7. 那为什么 Redis 也能存这个状态？

因为技术上完全可以。

例如：

```text
key:
iron:idempotency:REQ-10001
```

value：

```json
{
  "status": "PROCESSING",
  "owner": "A",
  "requestHash": "...",
  "startedAt": "...",
  "expireAt": "..."
}
```

Redis Lua 可以原子执行：

```text
如果 key 不存在
    → 创建 PROCESSING
    → 返回 ACQUIRED

如果 SUCCESS
    → 返回 SUCCESS + result

如果 PROCESSING
    → 返回 PROCESSING

如果 FAILED 且允许重试
    → 修改为 PROCESSING
    → 返回 ACQUIRED
```

优势就是：

> **读取状态 + 判断状态 + 状态转换可以在一个 Lua 中原子完成。**

例如概念上：

```lua
local value = redis.call('GET', KEYS[1])

if not value then
    redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
    return {'ACQUIRED'}
end

-- 解析并判断当前状态...
```

所以我之前说：

```text
Redis Lua IdempotencyRepository
```

是这个意思。

不是：

```text
Lua 自己保存状态
```

而是：

```text
Redis 保存状态

Lua 保证：
读取 + 判断 + 修改
原子执行
```

---

# 8. 你马上发现了真正的问题：Redis 状态会不会过期？

**会。**

这正是 Redis 方案最大的边界。

例如：

```text
REQ-10001
    ↓
SUCCESS
    ↓
Redis TTL = 24h
```

24 小时后：

```text
key 自动删除
```

此时客户端又发：

```text
REQ-10001
```

Redis：

```text
不存在
```

系统就会认为：

```text
第一次执行
```

然后再次创建订单。

所以你说：

> “状态应该放关系数据库”

在**业务幂等**场景下，我基本赞成。

---

# 9. 这里需要引入一个非常重要的概念：幂等窗口

不是所有幂等记录都需要永久存在。

例如 HTTP 接口：

```text
提交表单
```

产品要求：

> 10 分钟内相同 requestId 不能重复提交。

那么：

```text
Idempotency Window = 10 min
```

此时 Redis：

```text
TTL = 10 min
```

完全合理。

十分钟以后：

```text
同一个 Key 可以重新执行
```

这是产品定义允许的。

所以 Redis 并不是“状态会过期，所以一定错误”。

关键是：

> **业务允许不允许幂等状态过期。**

---

# 10. 我们把场景分成两类，你就非常容易理解了

## 第一类：短周期“防重复”

例如：

```text
用户疯狂双击提交按钮

1 秒内点了 5 次

网关 retry

客户端网络超时重试

消息短时间重复投递
```

需求可能只是：

```text
10 秒
1 分钟
10 分钟
1 小时
```

内不重复。

这种：

```text
Redis + TTL
```

非常合适。

例如：

```text
idempotencyKey = submit:user123:form456

TTL = 5min
```

5 分钟以后是否还能再次提交？

业务本来就：

```text
允许。
```

那完全没有问题。

这更准确地叫：

> **Duplicate Suppression / 去重窗口**

而不一定是永久业务幂等。

---

# 11. 第二类：真正的业务幂等

比如：

```text
支付
扣款
退款
转账
订单创建
发券
结算
提现
对账处理
```

这里要求：

```text
businessRequestNo = PAY-10001
```

**无论一天、一个月甚至几年以后再次出现，都不能重复扣款。**

这种我强烈倾向：

```text
关系型数据库
```

因为：

```text
持久
可查询
可审计
可备份
可恢复
可以参与事务
不会因为 TTL 自动消失
```

例如：

```sql
CREATE TABLE iron_idempotency_record (
    id BIGINT PRIMARY KEY,

    namespace VARCHAR(128) NOT NULL,

    idempotency_key VARCHAR(256) NOT NULL,

    request_hash VARCHAR(128),

    status VARCHAR(32) NOT NULL,

    owner_token VARCHAR(128),

    version BIGINT NOT NULL DEFAULT 0,

    result_data TEXT,

    error_code VARCHAR(128),

    processing_expire_at TIMESTAMP NULL,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP NOT NULL,

    completed_at TIMESTAMP NULL,

    UNIQUE KEY uk_namespace_key (
        namespace,
        idempotency_key
    )
);
```

这才像我们的生产级幂等组件。

---

# 12. 这里你还要区分两个“过期”

特别容易混。

## ① 幂等记录过期

例如：

```text
SUCCESS 记录过期
```

意味着：

```text
以后这个 key 可能再次被执行。
```

这是：

```text
Idempotency Record TTL
```

非常敏感。

---

## ② PROCESSING 执行权过期

这是另外一回事。

例如：

```text
A
↓
PROCESSING
↓
服务宕机
```

如果 PROCESSING 永远不释放：

```text
REQ-10001
```

以后永远：

```text
PROCESSING
```

系统卡死了。

所以 PROCESSING 通常需要：

```text
processingExpireAt
```

例如：

```text
status = PROCESSING

owner = A

processingExpireAt =
10:00:30
```

如果现在：

```text
10:01:00
```

系统就知道：

```text
这个 PROCESSING 已经过期
```

可以进入：

```text
恢复 / 重新抢占
```

所以：

> **PROCESSING 租约过期 ≠ 删除幂等记录。**

这是非常重要的区别。

---

# 13. 在数据库里也同样需要“过期”

所以即使你完全不用 Redis：

```text
MySQL IdempotencyRepository
```

也仍然需要：

```text
processingExpireAt
```

例如：

```sql
UPDATE iron_idempotency_record
SET owner_token = 'OWNER-B',
    processing_expire_at = NOW() + INTERVAL 30 SECOND,
    version = version + 1
WHERE idempotency_key = 'REQ-10001'
  AND status = 'PROCESSING'
  AND processing_expire_at < NOW();
```

B 条件更新：

```text
affectedRows = 1
```

表示：

> B 成功接管了这个已经死亡的 PROCESSING。

注意：

**不是删除记录。**

而是：

```text
PROCESSING
owner=A
        ↓ timeout
PROCESSING
owner=B
```

---

# 14. 这和我们刚做完的分布式锁是不是很像？

非常像。

分布式锁：

```text
ownerToken
leaseTime
expireAt
fencingToken
```

幂等状态：

```text
ownerToken
processingExpireAt
version
```

原因都是一样的：

> 分布式系统中的执行者可能突然死亡。

但二者保护的东西不同。

### Lock

保护：

```text
某一时间尽可能只有一个执行者。
```

### Idempotency State

记录：

```text
这个业务请求历史上到底执行到了哪里。
```

所以：

```text
Lock = 协调状态

Idempotency Record = 业务执行状态
```

这两个不能混为一谈。

---

# 15. 为什么关系数据库特别适合 Idempotency State？

因为它经常可以和业务事务放一起。

这其实是 DB 幂等最大的优势之一。

假设：

```text
创建订单
```

事务：

```java
@Transactional
public Order createOrder(...) {

    // 业务订单
    insertOrder(...);

    // 幂等状态
    markSuccess(...);

    return order;
}
```

可以做到：

```text
订单成功
+
Idempotency SUCCESS

同一个 DB Transaction
```

要么一起提交：

```text
COMMIT
```

要么一起：

```text
ROLLBACK
```

这非常强。

---

# 16. Redis 做不到这个天然原子性

如果：

```text
幂等状态在 Redis

业务订单在 MySQL
```

就会出现双写问题：

```text
MySQL insert order SUCCESS

                ↓

准备 Redis SET SUCCESS

                ↓

服务挂了
```

结果：

```text
订单已经创建

Redis：
PROCESSING
甚至 key 不存在
```

下一次请求进来：

```text
Redis 看起来没成功

于是重新创建订单
```

这就是典型：

```text
跨资源一致性
```

问题。

所以对于：

```text
业务数据最终落 MySQL
```

我通常更喜欢：

```text
幂等记录也在 MySQL
```

因为可以进入同一事务。

---

# 17. 这其实是决定 Repository 的一个很重要原则

我建议以后：

> **幂等状态尽量靠近最终业务事实的存储。**

例如业务数据主要在：

### MySQL

推荐：

```text
JdbcIdempotencyRepository
```

### PostgreSQL

也是：

```text
JdbcIdempotencyRepository
```

### Redis 本身就是业务事实

例如某些：

```text
临时 token
缓存构建
短任务去重
```

那么：

```text
RedisIdempotencyRepository
```

就非常合理。

---

# 18. 所以我们未来可能同时提供两个 Provider

这和 Distributed Lock 很像：

```text
idempotent-provider
├── idempotent-provider-jdbc
└── idempotent-provider-redis
```

统一 SPI：

```java
public interface IdempotencyRepository {

    IdempotencyAcquireResult tryAcquire(
            IdempotencyAcquireRequest request);

    IdempotencyRecord get(
            String namespace,
            String key);

    boolean markSuccess(
            IdempotencySuccessRequest request);

    boolean markFailed(
            IdempotencyFailureRequest request);
}
```

---

# 19. JDBC Provider 怎么做原子抢占？

第一次：

```sql
INSERT INTO iron_idempotency_record (
    namespace,
    idempotency_key,
    status,
    owner_token,
    processing_expire_at
)
VALUES (
    ?,
    ?,
    'PROCESSING',
    ?,
    ?
);
```

依赖：

```text
UNIQUE(namespace, idempotency_key)
```

只有一个线程 INSERT 成功。

其他线程：

```text
DuplicateKey
```

然后查询状态。

---

# 20. 已存在 FAILED / 过期 PROCESSING 怎么重新抢占？

不能：

```text
SELECT
然后
UPDATE
```

因为：

```text
A SELECT 可抢占
B SELECT 可抢占

A UPDATE
B UPDATE
```

还是并发。

必须：

```sql
UPDATE iron_idempotency_record
SET status = 'PROCESSING',
    owner_token = ?,
    processing_expire_at = ?,
    version = version + 1
WHERE namespace = ?
  AND idempotency_key = ?
  AND (
        status = 'FAILED'
        OR (
            status = 'PROCESSING'
            AND processing_expire_at < ?
        )
      );
```

然后：

```text
affectedRows == 1
```

才代表：

> **真正抢到了执行权。**

这就是我一直说的：

```text
条件更新 / CAS
```

---

# 21. SUCCESS 也不能随便覆盖

例如：

```sql
UPDATE iron_idempotency_record
SET status = 'SUCCESS',
    result_data = ?,
    completed_at = ?
WHERE namespace = ?
  AND idempotency_key = ?
  AND status = 'PROCESSING'
  AND owner_token = ?;
```

为什么要：

```text
owner_token = ?
```

假设：

```text
A PROCESSING
A 卡住

A processing lease 过期

B 接管
B SUCCESS

A 恢复
A markSuccess
```

如果没有 owner 条件：

```text
A 可能覆盖 B。
```

所以必须：

```text
ownerToken
```

甚至以后可以加入：

```text
version / fencingToken
```

---

# 22. 这跟我们 fencing 问题又完全接上了

例如：

```text
A
owner=A
version=10

A 卡住

B 接管
owner=B
version=11

B SUCCESS

A 恢复
```

A 执行：

```sql
UPDATE ...
WHERE owner_token = 'A'
  AND version = 10
```

更新：

```text
0 rows
```

A 就知道：

> 我的执行权已经失效。

这和 fencing 的思想是一致的。

---

# 23. 所以我建议未来 IdempotencyRecord 带 version

哪怕一期不马上把它暴露给业务。

例如：

```text
idempotency_key
status
owner_token
version
processing_expire_at
```

每次接管：

```text
version = version + 1
```

于是：

```text
A version 1

B version 2

C version 3
```

这实际上已经非常接近：

```text
Idempotency Fencing Version
```

以后如果需要：

```text
Business Resource
```

也拒绝旧执行者，就可以把 version/token 继续传下去。

---

# 24. 那么 Redis Provider 的 version 怎么做？

一样。

Lua：

```text
不存在
    ↓
创建：
status=PROCESSING
version=1

过期后重新抢占
    ↓
version=2
```

Lua 一次原子执行：

```text
检查状态
判断 expireAt
更新 owner
version++
```

所以 Redis 在原子状态机方面其实非常好用。

它的问题不是：

```text
无法实现状态机
```

而是：

```text
持久性
TTL
数据恢复
事务边界
审计能力
```

这些方面不如关系数据库适合严肃业务。

---

# 25. Redis SUCCESS 状态到底能不能永久保存？

技术上当然可以：

```text
不设置 TTL
```

Redis key 可以一直存在。

但我还是不会因此推荐用 Redis 永久保存资金类幂等状态。

原因包括：

```text
Redis 通常作为内存型基础设施
数据可能淘汰
运维可能 flush
集群迁移/恢复策略不同
持久化策略 RDB/AOF 可能不同
没有业务数据库那种审计习惯
内存成本更高
```

所以：

> **“Redis 可以永久保存”与“Redis 适合作为永久业务事实库”不是一回事。**

---

# 26. 所以你刚才说“状态都应该使用关系型数据库记录”——我会稍微修正一下

我会改成：

> **长期业务幂等状态，默认使用关系型数据库。短生命周期重复抑制状态，可以使用 Redis。**

这更准确。

例如：

| 场景                    | 推荐存储  |
| --------------------- | ----- |
| 5 秒内防止按钮连点            | Redis |
| 30 秒内防止接口重复 submit    | Redis |
| 网关 requestId 10 分钟防重复 | Redis |
| Cache rebuild 防重复     | Redis |
| 短生命周期任务去重             | Redis |
| 创建订单                  | DB    |
| 支付请求                  | DB    |
| 提现                    | DB    |
| 转账                    | DB    |
| 退款                    | DB    |
| 发券业务                  | DB    |
| 结算                    | DB    |
| 消息产生不可重复业务结果          | DB 更稳 |

---

# 27. 我甚至建议幂等组件把两个概念正式区分

未来不要所有东西都叫：

```text
Idempotency
```

可以明确两个使用模型。

### `DEDUPE`

```text
短时间重复抑制
允许 TTL 以后重新执行
```

例如：

```java
IdempotencyMode.DEDUPE
```

适合 Redis。

### `DURABLE`

```text
持久业务幂等
历史结果必须长期可信
```

例如：

```java
IdempotencyMode.DURABLE
```

默认要求 JDBC。

不过一期是否把 Mode 暴露给用户，我还要再斟酌，避免 API 过早复杂化。

---

# 28. 最推荐的数据库执行流程

以后我们幂等一期，我会优先设计这条主链：

```text
请求
 │
 ▼
生成 IdempotencyKey
 │
 ▼
读取/抢占 IdempotencyRecord
 │
 ├──────── SUCCESS
 │             │
 │             └── 返回历史结果
 │
 ├──────── PROCESSING
 │             │
 │             └── WAIT / REJECT / RETURN_PROCESSING
 │
 ├──────── FAILED
 │             │
 │             └── 根据策略重新抢占
 │
 └──────── NOT_EXIST
               │
               ▼
        原子创建 PROCESSING
               │
               ▼
            callback
          /           \
        成功            失败
         │               │
         ▼               ▼
      SUCCESS          FAILED
         │
         ▼
       返回结果
```

这里：

```text
分布式锁
```

可以放在外围：

```text
DistributedLock
        ↓
Idempotency State Machine
```

但即便没有锁：

```text
Idempotency State Machine
```

本身也必须正确。

---

# 29. 如果用了 Redisson，整个结构会是什么？

未来我真正推荐：

```text
                     IdempotencyExecutor
                              │
                ┌─────────────┴─────────────┐
                │                           │
              optional                    required
                │                           │
                ▼                           ▼
      DistributedLockClient       IdempotencyRepository
                │                           │
                ▼                           ▼
       RedissonLockProvider       JdbcIdempotencyRepository
```

也就是说生产默认：

```text
Redisson
    ↓
做并发协调

MySQL
    ↓
保存真实幂等状态
```

这其实比：

```text
Redisson Lock
+
Redis Idempotency State
```

更符合你这种技术中台组件的长期定位。

---

# 30. 再进一步：订单数据和 IdempotencyRecord 可以同事务

这是我最喜欢 JDBC 方案的地方。

例如：

```java
@Transactional
public Order executeBusiness(...) {

    Order order = orderRepository.create(...);

    idempotencyRepository.markSuccess(
            key,
            owner,
            serialize(order)
    );

    return order;
}
```

如果：

```text
订单 insert
```

失败：

```text
ROLLBACK
```

幂等 SUCCESS 也不会留下。

如果：

```text
markSuccess
```

失败：

```text
订单同样 rollback
```

于是：

```text
业务事实
+
幂等事实
```

高度一致。

这比 Redis 双写简单太多。

---

# 31. 但是这里还存在一个我们后面必须认真设计的问题

假设：

```text
IdempotencyExecutor
    ↓
开 PROCESSING
    ↓
业务 Transaction
```

到底：

```text
PROCESSING
```

和：

```text
业务 transaction
```

是不是同一个事务？

这个答案并不是简单的“是”。

因为如果：

```text
PROCESSING
```

和整个业务放在一个长事务里：

```text
其他线程可能看不到 PROCESSING
```

它们可能继续竞争。

所以未来很可能需要：

```text
REQUIRES_NEW
```

或者：

```text
独立短事务抢占 PROCESSING

业务事务执行

独立/同业务事务标记 SUCCESS
```

这个会成为幂等组件一期非常核心的一章。

这里先记住即可，我们后面专门设计。

---

# 32. 我现在会调整之前给你的推荐表

之前我说：

```text
普通接口防重复
Redisson + Redis Lua
```

现在更严谨地改成：

| 场景                   | Lock               | Idempotency State |
| -------------------- | ------------------ | ----------------- |
| UI 连点/短时间 request 去重 | 可无锁 / Redisson     | Redis             |
| 短生命周期 API 去重         | Redisson 可选        | Redis             |
| 普通有业务结果 API          | Redisson           | **JDBC 优先**       |
| 创建订单                 | Redisson           | **JDBC**          |
| 支付/资金                | Redisson + fencing | **JDBC**          |
| 消息消费业务幂等             | Redisson 可选        | **JDBC 优先**       |
| Cache rebuild        | Redisson           | Redis             |
| 临时任务防重复              | Redisson           | Redis/DB 视生命周期    |
| 长生命周期任务              | Redisson           | DB                |

这样更准确。

---

# 33. 最终你可以把 Idempotency State 理解成一句话

> **Idempotency State 不是“锁的状态”，而是“某个业务幂等键所代表的逻辑请求，历史上执行到了什么阶段，以及最终得到了什么结果”。**

它解决的是：

```text
这个请求：

有没有执行过？
正在谁执行？
执行成功了吗？
失败了吗？
能不能再执行？
上一次结果是什么？
当前执行权是否已经过期？
当前这个执行者还是不是合法 owner？
```

而存储选择是：

```text
                    Idempotency State
                           │
              ┌────────────┴────────────┐
              ▼                         ▼
            Redis                     JDBC
              │                         │
      短生命周期 / 高性能          长期业务事实 / 可审计
      TTL 天然适合                 强持久化
      Lua 原子状态机               Unique + CAS
      不适合长期业务事实           可与业务事务结合
```

**对于我们接下来真正要做的幂等组件，我目前已经更倾向：`JdbcIdempotencyRepository` 作为生产默认实现，`RedisIdempotencyRepository` 作为短生命周期高性能实现；Redisson 只是可选的并发协调层。**

这会比“所有幂等状态都放 Redis”稳得多。
