package com.xjtu.iron.distributed.lock.core.fencing;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultFencingTokenProviderRegistryTest {

    @Test
    void shouldFindProviderOnlyByExplicitName() {
        FencingTokenProvider jdbc = provider("jdbc-sequence");
        DefaultFencingTokenProviderRegistry registry =
                new DefaultFencingTokenProviderRegistry(List.of(jdbc));

        assertThat(registry.findProvider("jdbc-sequence")).contains(jdbc);
        assertThat(registry.findProvider("missing")).isEmpty();
        assertThat(registry.providerNames()).containsExactly("jdbc-sequence");
    }

    @Test
    void blankProviderNameShouldNotImplicitlySelectSoleProvider() {
        DefaultFencingTokenProviderRegistry registry =
                new DefaultFencingTokenProviderRegistry(List.of(provider("jdbc-sequence")));

        assertThat(registry.findProvider(null)).isEmpty();
        assertThat(registry.findProvider("   ")).isEmpty();
    }

    @Test
    void duplicateProviderNameShouldFailFast() {
        assertThatThrownBy(() -> new DefaultFencingTokenProviderRegistry(
                List.of(provider("jdbc"), provider("jdbc"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate fencing token provider");
    }

    private FencingTokenProvider provider(String name) {
        return new FencingTokenProvider() {
            @Override public String providerName() { return name; }
            @Override public boolean supports(FencingTokenRequest request) { return true; }
            @Override public FencingTokenResponse nextToken(FencingTokenRequest request) {
                return FencingTokenResponse.issued(1L);
            }
        };
    }
}
