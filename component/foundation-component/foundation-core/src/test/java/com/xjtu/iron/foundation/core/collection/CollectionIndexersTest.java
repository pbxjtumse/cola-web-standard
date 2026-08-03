package com.xjtu.iron.foundation.core.collection;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CollectionIndexersTest {

    @Test
    void shouldRejectDuplicateIndexKeys() {
        assertThrows(IllegalArgumentException.class,
                () -> CollectionIndexers.uniqueIndex(List.of("aa", "ab"), value -> value.substring(0, 1)));
    }

    @Test
    void shouldGroupAndPreserveOrder() {
        assertEquals(List.of("a1", "a2"),
                CollectionIndexers.groupBy(List.of("a1", "b1", "a2"), value -> value.substring(0, 1)).get("a"));
    }
}
