package com.xjtu.iron.foundation.core.text;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringUtilsTest {

    @Test
    void truncateShouldKeepCompleteCodePoint() {
        assertThat(StringUtils.truncateWithSuffix("ab😀cd", 4, "...")).isEqualTo("a...");
    }
}
