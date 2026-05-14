# 零、一些思考
1. 下游接口慢，把线程池拖满
2. 重试乱打，把下游打挂
3. 没有限流，流量一上来自己先死
4. 没有熔断，下游故障时持续雪崩
5. 没有统一异常映射，调用方不知道是超时、限流、熔断还是业务失败
6. 没有观测标签，出了问题不知道是哪个下游导致的

一、依赖原则
    governance-core 不能依赖 Resilience4j
    governance-core 不能依赖 Sentinel
    governance-core 不能依赖 Nacos
    governance-api 不能依赖 Spring
    业务代码不能直接依赖 Resilience4j