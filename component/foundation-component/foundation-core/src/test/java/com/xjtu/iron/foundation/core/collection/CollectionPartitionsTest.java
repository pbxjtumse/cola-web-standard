package com.xjtu.iron.foundation.core.collection;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CollectionPartitionsTest {

    @Test
    void shouldCreateIndependentImmutableBatches() {
        List<List<Integer>> batches = CollectionPartitions.partition(List.of(1, 2, 3, 4, 5), 2);
        assertEquals(List.of(List.of(1, 2), List.of(3, 4), List.of(5)), batches);
        assertThrows(UnsupportedOperationException.class, () -> batches.get(0).add(9));
    }
}
