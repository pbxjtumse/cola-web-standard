package com.xjtu.iron.concurrency.api.enums;

/**
 * 线程池工作队列类型。
 */
public enum QueueType {

    /**
     * 有界 LinkedBlockingQueue。
     *
     * <p>普通业务异步任务默认推荐。</p>
     *
     * <p>特点：</p>
     * <pre>
     * 1. 链表结构；
     * 2. 必须配置 queueCapacity；
     * 3. 任务会先进入队列；
     * 4. 队列满了才扩容到 maxPoolSize。
     * </pre>
     * <p> 适用场景 </p>
     * <pre>
     * 1.查询用户
     * 2.查询订单
     * 3.组装数据
     * 4.异步通知
     * 5.普通后台任务
     * </pre>
     */
    BOUNDED_LINKED_BLOCKING_QUEUE,

    /**
     * 有界 ArrayBlockingQueue。
     *
     * <p>固定数组结构，容量固定。</p>
     *
     * <p>特点：</p>
     * <pre>
     * 1. 内存更可控；
     * 2. 吞吐稳定；
     * 3. 适合对队列容量非常敏感的场景。
     * </pre>
     * <p>适用场景</p>
     * <pre> 对内存和容量 非常敏感</pre>
     */
    BOUNDED_ARRAY_BLOCKING_QUEUE,

    /**
     * SynchronousQueue。
     *
     * <p>不存储任务，任务必须直接交给工作线程。</p>
     *
     * <p>特点：</p>
     * <pre>
     * 1. queueCapacity 无意义；
     * 2. 更容易快速扩容到 maxPoolSize；
     * 3. 超过 maxPoolSize 后直接触发拒绝；
     * 4. 适合低排队、高并发、快速失败场景。
     * </pre>
     */
    DIRECT_HANDOFF
}