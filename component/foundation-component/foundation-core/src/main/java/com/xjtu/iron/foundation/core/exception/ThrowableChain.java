package com.xjtu.iron.foundation.core.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * 提供异常原因链的循环安全遍历。
 */
public final class ThrowableChain {

    private ThrowableChain() {
    }

    /**
     * 从最外层异常开始返回完整原因链。
     */
    public static List<Throwable> toList(Throwable throwable) {
        if (throwable == null) {
            return List.of();
        }
        List<Throwable> result = new ArrayList<>();
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = throwable;
        while (current != null && visited.add(current)) {
            result.add(current);
            current = current.getCause();
        }
        return List.copyOf(result);
    }
}
