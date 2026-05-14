package com.xjtu.iron.governance.demo.client;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class MockCouponClient {

    public String queryCoupon(Long userId) {
        int random = ThreadLocalRandom.current().nextInt(100);

        if (random < 70) {
            throw new RuntimeException("mock coupon-service error");
        }

        return "coupon-list-for-user-" + userId;
    }

    public String slowQuery(Long userId) {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return "slow-coupon-list-for-user-" + userId;
    }
}