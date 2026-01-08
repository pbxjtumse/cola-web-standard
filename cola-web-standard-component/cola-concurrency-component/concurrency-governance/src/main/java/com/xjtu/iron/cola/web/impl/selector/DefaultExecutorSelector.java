package com.xjtu.iron.cola.web.impl.selector;

import com.xjtu.iron.cola.web.ExecutorSelector;
import com.xjtu.iron.cola.web.context.GovernorContext;
import com.xjtu.iron.cola.web.registry.ThreadPoolRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
public class DefaultExecutorSelector implements ExecutorSelector {

    private final ThreadPoolRegistry registry;

    public DefaultExecutorSelector(ThreadPoolRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Executor select(GovernorContext context) {
        // infra 决定：order 业务用哪个线程池 biz这个key就是线程池  或者建议在枚举中设置这个
        String biz = context.tags().get("biz");
        return registry.get(biz);
    }
}

