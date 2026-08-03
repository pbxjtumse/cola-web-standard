package com.xjtu.iron.foundation.core.exception;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExceptionSupportTest {

    @Test
    void shouldUnwrapAsyncException() {
        IllegalStateException cause = new IllegalStateException("failed");
        assertInstanceOf(IllegalStateException.class, ExceptionSupport.unwrapAsync(new CompletionException(cause)));
        assertTrue(ExceptionSupport.contains(new RuntimeException(cause), IllegalStateException.class));
    }
}
