# 对外稳定抽象层

| 抽象                     | 作用                            |
|-------------------------|---------------------------------|
| CacheClient            | 统一缓存访问入口                  |
| CacheKey               | 统一 key 模型                     |
| CacheSpec              | 缓存声明模型                      |
| CacheValue             | 缓存值包装                        |
| CacheResult            | 缓存访问结果                      |
| CacheLoader            | 缓存未命中时的数据加载逻辑          |
| CachePolicy            | 缓存策略                          |
| CacheLevel             | 缓存层级                          |
| CacheOperation         | get / put / evict / refresh     |
| CacheConsistencyPolicy | 一致性策略                        |
| CacheProtectPolicy     | 保护策略                          |
## 这一层的目标是 
业务只表达“我要缓存什么、key 是什么、TTL 是多少、miss 后如何加载”，不关心底层到底走本地缓存、Redis，还是二级缓存。