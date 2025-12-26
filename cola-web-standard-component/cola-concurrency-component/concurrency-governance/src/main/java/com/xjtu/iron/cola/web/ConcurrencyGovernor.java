package com.xjtu.iron.cola.web;

import com.xjtu.iron.cola.web.context.GovernorContext;
import com.xjtu.iron.cola.web.dto.Permit;

/**
 * 1.Governor 不能感知“业务语义”，但可以感知“业务标签” Governor 只认 context，不认业务对象
 * 2.没有 execute 、 Runnable、Executor、在任务提交前，只判断是否允许执行
 * 3. 它不关心：线程池实现 队列类型 拒绝策略 ，它只输出一个结果，允许 → 提交  拒绝 → 你来决定怎么处理
 * @author pbxjtu
 */
public interface ConcurrencyGovernor {

    /**
     * 尝试获取执行许可
     */
    Permit tryAcquire(GovernorContext context);

}

