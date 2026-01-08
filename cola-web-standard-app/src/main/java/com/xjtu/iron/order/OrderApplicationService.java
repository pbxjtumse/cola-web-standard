package com.xjtu.iron.order;

//package by domain, not by duty


import com.xjtu.iron.cola.web.ExecutorSelector;
import com.xjtu.iron.cola.web.GovernanceExecutor;
import com.xjtu.iron.cola.web.context.GovernorContext;
import com.xjtu.iron.domain.order.CreateOrderCommand;
import com.xjtu.iron.domain.order.Order;
import com.xjtu.iron.domain.order.OrderDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.Executor;

@Service
public class OrderApplicationService{
    @Autowired
    private  GovernanceExecutor governanceExecutor;
    @Autowired
    private  ExecutorSelector executorSelector;
    @Autowired
    private  OrderDomainService domainService;

    public Order createOrder(CreateOrderCommand cmd) throws Exception {

        //在app层能够获得线程池信息 在领域层不获取这些信息
        GovernorContext context = GovernorContext.builder()
                .api("order.create")
                .biz("order")
                .build();

        Executor executor = executorSelector.select(context);

        return governanceExecutor.execute(
                context,
                () -> domainService.create(cmd),
                executor
        );
    }
}