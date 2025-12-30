package com.xjtu.iron.order;

//package by domain, not by duty


import com.xjtu.iron.cola.web.ExecutorSelector;
import com.xjtu.iron.cola.web.GovernanceExecutor;
import com.xjtu.iron.cola.web.context.GovernorContext;
import com.xjtu.iron.domain.order.CreateOrderCommand;
import com.xjtu.iron.domain.order.Order;
import com.xjtu.iron.domain.order.OrderDomainService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.Executor;

public class OrderApplicationService{
    private final GovernanceExecutor governanceExecutor;
    private final ExecutorSelector executorSelector;
    private final OrderDomainService domainService;

    public OrderApplicationService(
            GovernanceExecutor governanceExecutor,
            ExecutorSelector executorSelector,
            OrderDomainService domainService) {
        this.governanceExecutor = governanceExecutor;
        this.executorSelector = executorSelector;
        this.domainService = domainService;
    }

    public Order createOrder(CreateOrderCommand cmd) throws Exception {

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