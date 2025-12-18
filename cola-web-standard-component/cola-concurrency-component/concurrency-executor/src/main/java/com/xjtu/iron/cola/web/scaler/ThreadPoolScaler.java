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
 *  线程池的“动态”本质是“参数可调”，不是“结构可变”
 * 只支持 poolName，不支持 tag ，tag 级扩缩容是 Governor / Ops 的事，不是 Scaler，Scaler 操作的是“实体资源”
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


