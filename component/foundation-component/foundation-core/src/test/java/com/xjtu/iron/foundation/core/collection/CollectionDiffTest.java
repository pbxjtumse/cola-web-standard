package com.xjtu.iron.foundation.core.collection;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CollectionDiffTest {

    @Test
    void shouldDescribeAddedRemovedAndRetainedValues() {
        CollectionDifference<Integer> difference = CollectionDiff.compare(List.of(1, 2, 3), List.of(2, 3, 4));
        assertEquals(List.of(4), difference.getAdded());
        assertEquals(List.of(1), difference.getRemoved());
        assertEquals(List.of(2, 3), difference.getRetained());
    }
}
