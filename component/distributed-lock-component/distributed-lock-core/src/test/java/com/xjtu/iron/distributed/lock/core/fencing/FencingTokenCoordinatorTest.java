package com.xjtu.iron.distributed.lock.core.fencing;

import com.xjtu.iron.distributed.lock.api.LockOptions;
import com.xjtu.iron.distributed.lock.core.spi.LockProvider;
import com.xjtu.iron.distributed.lock.core.spi.LockProviderCapabilities;
import com.xjtu.iron.distributed.lock.core.spi.request.LockAcquireRequest;
import com.xjtu.iron.distributed.lock.core.spi.request.LockCheckRequest;
import com.xjtu.iron.distributed.lock.core.spi.request.LockReleaseRequest;
import com.xjtu.iron.distributed.lock.core.spi.request.LockRenewRequest;
import com.xjtu.iron.distributed.lock.core.spi.response.LockAcquireResponse;
import com.xjtu.iron.distributed.lock.core.spi.response.LockCheckResponse;
import com.xjtu.iron.distributed.lock.core.spi.response.LockReleaseResponse;
import com.xjtu.iron.distributed.lock.core.spi.response.LockRenewResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FencingTokenCoordinatorTest {

    @Test
    void shouldChooseNativeProviderWhenSupported() {
        FencingTokenCoordinator coordinator = new FencingTokenCoordinator(
                new DefaultFencingTokenProviderRegistry(List.of()));
        FencingTokenPlan plan = coordinator.plan(lockProvider(true),
                LockOptions.builder().fencingRequired(true).build());
        assertThat(plan.mode()).isEqualTo(FencingTokenMode.NATIVE);
    }

    @Test
    void explicitLockProviderNameShouldChooseNativeFencing() {
        FencingTokenCoordinator coordinator = new FencingTokenCoordinator(
                new DefaultFencingTokenProviderRegistry(List.of()));
        FencingTokenPlan plan = coordinator.plan(lockProvider(true),
                LockOptions.builder().fencingRequired(true)
                        .fencingTokenProviderName("lock").build());
        assertThat(plan.mode()).isEqualTo(FencingTokenMode.NATIVE);
    }

    @Test
    void explicitExternalProviderShouldOverrideNativeSupport() {
        FencingTokenProvider external = provider("jdbc-sequence", 10L);
        FencingTokenCoordinator coordinator = new FencingTokenCoordinator(
                new DefaultFencingTokenProviderRegistry(List.of(external)));
        FencingTokenPlan plan = coordinator.plan(lockProvider(true),
                LockOptions.builder().fencingRequired(true)
                        .fencingTokenProviderName("jdbc-sequence").build());
        assertThat(plan.mode()).isEqualTo(FencingTokenMode.EXTERNAL);
        assertThat(plan.externalProvider()).contains(external);
    }

    @Test
    void shouldFailFastWhenNoFencingSourceAvailable() {
        FencingTokenCoordinator coordinator = new FencingTokenCoordinator(
                new DefaultFencingTokenProviderRegistry(List.of()));
        assertThatThrownBy(() -> coordinator.plan(lockProvider(false),
                LockOptions.builder().fencingRequired(true).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("configure fencingTokenProviderName explicitly");
    }

    @Test
    void shouldNotGuessDefaultExternalProviderWhenProviderNameIsMissing() {
        FencingTokenProvider external = provider("jdbc-sequence", 10L);
        FencingTokenCoordinator coordinator = new FencingTokenCoordinator(
                new DefaultFencingTokenProviderRegistry(List.of(external)));

        assertThatThrownBy(() -> coordinator.plan(lockProvider(false),
                LockOptions.builder().fencingRequired(true).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("configure fencingTokenProviderName explicitly");
    }

    private FencingTokenProvider provider(String name, long token) {
        return new FencingTokenProvider() {
            @Override public String providerName() { return name; }
            @Override public boolean supports(FencingTokenRequest request) { return true; }
            @Override public FencingTokenResponse nextToken(FencingTokenRequest request) {
                return FencingTokenResponse.issued(token);
            }
        };
    }

    private LockProvider lockProvider(boolean nativeFencing) {
        return new LockProvider() {
            @Override public String providerName() { return "lock"; }
            @Override public LockAcquireResponse acquire(LockAcquireRequest request) { throw new UnsupportedOperationException(); }
            @Override public LockReleaseResponse release(LockReleaseRequest request) { throw new UnsupportedOperationException(); }
            @Override public LockRenewResponse renew(LockRenewRequest request) { throw new UnsupportedOperationException(); }
            @Override public LockCheckResponse check(LockCheckRequest request) { throw new UnsupportedOperationException(); }
            @Override public LockProviderCapabilities capabilities() {
                return LockProviderCapabilities.builder().fencingTokenSupported(nativeFencing).build();
            }
        };
    }
}
