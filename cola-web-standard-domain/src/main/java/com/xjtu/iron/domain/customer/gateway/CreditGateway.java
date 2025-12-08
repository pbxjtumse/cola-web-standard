package com.xjtu.iron.domain.customer.gateway;

import com.xjtu.iron.domain.customer.Credit;

//Assume that the credit info is in another distributed Service
public interface CreditGateway {
    Credit getCredit(String customerId);
}
