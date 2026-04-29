package com.xjtu.iron.cola.web.scaler;


/**

 * | 项目           | 能否动态       |
 * | ------------- | ------------- |
 * | corePoolSize  | ✅           |
 * | maxPoolSize   | ✅           |
 * | keepAlive     | ✅           |
 * | queue 类型     | ❌           |
 * | queue 容量     | ⚠️（需重建）   |
 * | ThreadFactory | ❌           |
 *  线程池的“动态”本质是“参数可调”，不是“结构可变” 它不创建线程池、不注册、不做治理判断。
 *  只支持 poolName，不支持 tag ，tag 级扩缩容是 Governor / Ops 的事，不是 Scaler，Scaler 操作的是“实体资源”
 *
 *
 * 如果没有 Scaler，你的 executor 体系就永远是“一次性配置”。
 * 而现实世界是：
 * 白天流量高，线程池要大
 * 夜里流量低，线程池要缩
 * MQ 堆积时，需要临时扩容
 * 下游雪崩时，需要立刻收缩
 *  这些都不是 Governor 该干的事。
 *  Governor 是“策略”，Scaler 是“资源形态控制”
 *
 * @author pangbo
 * @date 2025/12/18
 */
public interface ThreadPoolScaler {

    /**
     * @param poolName
     * @param newCoreSize
     */
    void resizeCorePoolSize(String poolName, int newCoreSize);

    /**
     * @param poolName
     * @param newMaxSize
     */
    void resizeMaxPoolSize(String poolName, int newMaxSize);

    /**
     * @param poolName
     * @param newCoreSize
     * @param newMaxSize
     */
    void resize(String poolName, int newCoreSize, int newMaxSize);

}


