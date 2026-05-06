一、依赖原则
    governance-core 不能依赖 Resilience4j
    governance-core 不能依赖 Sentinel
    governance-core 不能依赖 Nacos
    governance-api 不能依赖 Spring
    业务代码不能直接依赖 Resilience4j