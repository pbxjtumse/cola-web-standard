package com.xjtu.iron.foundation.core.collection;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ListUtilsTest {

    @Test
    void partitionShouldReturnImmutableCopies() {
        List<List<Integer>> partitions = ListUtils.partition(List.of(1, 2, 3), 2);
        assertThat(partitions).containsExactly(List.of(1, 2), List.of(3));
    }
}
