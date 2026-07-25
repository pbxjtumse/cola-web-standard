package com.xjtu.iron.distributed.lock.core.spi.request;

import com.xjtu.iron.distributed.lock.api.LockOptions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LockAcquireRequestTest {

    @Test
    void nativeFencingShouldDefaultToFalseEvenWhenFencingIsRequired() {
        LockAcquireRequest request = LockAcquireRequest.builder()
                .lockName("order:1")
                .ownerToken("owner")
                .options(LockOptions.builder().fencingRequired(true).build())
                .build();

        assertThat(request.isNativeFencingRequired()).isFalse();
    }

    @Test
    void nativeFencingShouldBeTrueOnlyWhenExplicitlySetByCorePlan() {
        LockAcquireRequest request = LockAcquireRequest.builder()
                .lockName("order:1")
                .ownerToken("owner")
                .options(LockOptions.builder().fencingRequired(true).build())
                .nativeFencingRequired(true)
                .build();

        assertThat(request.isNativeFencingRequired()).isTrue();
    }
}
