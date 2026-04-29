package com.xjtu.iron.domain.customer.gateway;

import com.xjtu.iron.domain.customer.model.Customer;

public interface CustomerGateway {
    Customer getByById(String customerId);
}
