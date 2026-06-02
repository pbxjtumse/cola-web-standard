# 一 注意问题
1. 缓存一致性问题
2. 本地缓存和 Redis 缓存不一致
3. 缓存击穿导致 DB 瞬间被打爆
4. 缓存雪崩
5. 缓存预热复杂
6. 删除缓存和更新数据库时序问题
7. 多级缓存失效广播问题
8. key 规范混乱
9. 缓存污染

我能统一使用缓存
我能用 Caffeine + Redis 二级缓存
我能防穿透、防单机击穿、防雪崩
多实例本地缓存不一致怎么办？
热点 key 多实例同时击穿怎么办？
缓存策略能不能动态调整？
Redis 抖动时能不能被治理？
缓存事件能不能统一管理？

4. 二期能力边界

我建议二期只做这 6 个方向。

4.1 本地缓存失效通知

解决问题：

服务有多个实例：
A 实例更新数据并 evict 缓存；
B / C / D 实例的 Caffeine 本地缓存还在；
导致 B / C / D 短时间读到旧数据。

二期要做：

A 实例删除缓存后，发布缓存失效事件；
其他实例收到事件后，只删除本地 Caffeine；
Redis 不重复删，避免事件风暴。

这是二期最重要的能力。

4.2 分布式互斥加载

一期的 LocalMutexCacheLoadGuard 只能解决单实例内并发击穿。

但是多实例场景下：

实例 A miss
实例 B miss
实例 C miss

三个实例都会查数据库。

二期要支持 Redis 分布式锁：

同一个 key 多个实例同时 miss；
只有一个实例获得加载权；
其他实例短暂等待，然后重新查 Redis。

这解决多实例缓存击穿。

4.3 动态缓存策略

一期的 CacheSpec 来自配置文件。

二期应该支持：

cacheName 级别动态调整：
- TTL
- 是否开启 L1
- 是否开启 L2
- 是否开启空值缓存
- 是否开启互斥加载
- 降级策略
- 本地缓存容量

注意：

二期先做动态策略模型和刷新机制，不一定第一版就接 Nacos。

可以先把抽象做好，再接 Nacos / Apollo。

4.4 缓存事件模型

二期不能到处散落：

publish invalidation
subscribe invalidation
spec changed
refresh
evict

要有统一事件模型。

比如：

CacheEvent
CacheEventType
CacheEventPublisher
CacheEventSubscriber
CacheEventHandler

后面无论是 Redis Pub/Sub、MQ、还是本地事件，都能复用这套模型。

4.5 governance-component 接入

你已经有 governance-component 方向。

缓存组件二期应该接入它，但不要自己重新造熔断限流。

缓存组件只做适配：

Redis get/set/del 走治理执行器
loader 加载可以走治理执行器
热点 key 可以走限流策略

比如资源名：

cache.redis.get.campaignRule
cache.redis.set.campaignRule
cache.loader.campaignRule

这样后面 Redis 抖动、loader 慢、热点 key 打爆，都有治理入口。

4.6 缓存慢日志与事件指标

一期只有基础 Micrometer 指标。

二期要增强：

1. Redis get 慢日志
2. Redis set 慢日志
3. loader 慢日志
4. 事件发布失败日志
5. 事件消费失败日志
6. 分布式锁等待耗时
7. 本地缓存失效次数

这不是为了好看，是为了后面排查问题。

缓存组件最怕的问题是：

看起来用了缓存，但不知道命中在哪里，不知道谁删了，不知道为什么没命中。
5. 二期不建议做的内容

下面这些不要放二期第一版。

1. 三级缓存
2. BloomFilter
3. 自动热点 key 识别
4. 逻辑过期
5. 缓存控制台
6. 多 Redis 集群
7. 复杂注解缓存
8. 大规模批量加载框架

获取锁的过程
请求进入
↓
查 L1 miss
↓
查 L2 miss
↓
尝试获取 Redis 锁
↓
获取成功：
执行 loader
写 Redis
写 L1
释放锁



本地缓存失效通知
service-A-1 调用 cacheClient.evict(key)
↓
删除 service-A-1 本地 Caffeine
↓
删除 Redis
↓
发布 CacheInvalidationEvent 到 Redis Pub/Sub
↓
service-A-2 收到事件
↓
删除 service-A-2 本地 Caffeine

service-A-3 收到事件
↓
删除 service-A-3 本地 Caffeine

需要注意 其他实例收到事件后只删除 L1，不再删除 Redis，不再继续发布事件。 



获取失败：
等待短时间
重试查 Redis
Redis 仍 miss：
根据策略继续等待 / 降级 / 自己加载