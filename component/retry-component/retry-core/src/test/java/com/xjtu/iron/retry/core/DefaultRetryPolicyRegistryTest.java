package com.xjtu.iron.retry.core;

import com.xjtu.iron.retry.api.RetryPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultRetryPolicyRegistryTest {

    @Test
    void shouldRegisterAndResolvePolicy() {
        DefaultRetryPolicyRegistry registry = new DefaultRetryPolicyRegistry();
        RetryPolicy policy = RetryPolicy.builder("registered").build();

        registry.register(policy);

        assertSame(policy, registry.getRequired("registered"));
    }

    @Test
    void shouldFailWhenRequiredPolicyDoesNotExist() {
        DefaultRetryPolicyRegistry registry = new DefaultRetryPolicyRegistry();

        assertThrows(IllegalArgumentException.class, () -> registry.getRequired("missing"));
    }
}
