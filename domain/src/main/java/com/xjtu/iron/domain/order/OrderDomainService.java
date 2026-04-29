package com.xjtu.iron.domain.order;

import org.springframework.stereotype.Service;

@Service
public class OrderDomainService {

    public Order create(CreateOrderCommand cmd) {
        // 纯业务规则
        Order order = new Order(cmd.getUserId(), cmd.getSkuId());
        return order;
    }
}
