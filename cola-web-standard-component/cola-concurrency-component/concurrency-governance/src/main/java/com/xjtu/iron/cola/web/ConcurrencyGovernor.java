package com.xjtu.iron.cola.web;

import com.xjtu.iron.cola.web.context.GovernorContext;
import com.xjtu.iron.cola.web.dto.Permit;

/**
 * 1.Governor 不能感知“业务语义”，但可以感知“业务标签” Governor 只认 context，不认业务对象
 * 2.没有 execute 、 Runnable、Executor、只做「是否允许」的判断 不关心：线程池实现 队列类型  拒绝策略
 * @author pbxjtu
 */
public interface ConcurrencyGovernor {

    /**
     * 尝试获取执行许可
     */
    Permit tryAcquire(GovernorContext context);

}

