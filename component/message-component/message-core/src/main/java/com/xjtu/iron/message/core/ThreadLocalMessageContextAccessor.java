package com.xjtu.iron.message.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 ThreadLocal 栈实现当前入站消息上下文。
 *
 * <p>一期只保证同步 Handler 调用范围内可见。跨线程传播应由并行组件的上下文传播
 * 集成完成，不能假设普通 ThreadLocal 会自动跨线程。</p>
 */
public final class ThreadLocalMessageContextAccessor implements MessageContextAccessor {

    /** 每个线程独立维护嵌套消息上下文栈。 */
    private final ThreadLocal<Deque<CurrentMessage>> contexts =
            ThreadLocal.withInitial(ArrayDeque::new);

    /**
     * 返回当前栈顶消息。
     */
    @Override
    public Optional<CurrentMessage> current() {
        // 获取当前线程的上下文栈。
        Deque<CurrentMessage> stack = contexts.get();
        // 栈为空时返回 Optional.empty。
        return Optional.ofNullable(stack.peek());
    }

    /**
     * 压入新的当前消息上下文。
     */
    @Override
    public Scope open(CurrentMessage currentMessage) {
        // 当前消息不能为空。
        Objects.requireNonNull(currentMessage, "currentMessage must not be null");
        // 获取当前线程上下文栈。
        Deque<CurrentMessage> stack = contexts.get();
        // 将新消息压入栈顶，支持嵌套消费或测试调用。
        stack.push(currentMessage);
        // 防止同一个 Scope 被重复关闭。
        AtomicBoolean closed = new AtomicBoolean(false);
        // 返回关闭作用域的 Lambda。
        return () -> {
            // 仅第一次关闭时执行恢复。
            if (closed.compareAndSet(false, true)) {
                // 获取当前线程实际上下文栈。
                Deque<CurrentMessage> currentStack = contexts.get();
                // 栈为空或顶部不是当前消息说明作用域关闭顺序错误。
                if (currentStack.isEmpty() || currentStack.peek() != currentMessage) {
                    // 直接抛错，避免静默破坏后续消息关联关系。
                    throw new IllegalStateException("message context scopes must close in LIFO order");
                }
                // 移除当前作用域。
                currentStack.pop();
                // 栈完全为空时移除 ThreadLocal，避免线程池线程长期持有空栈。
                if (currentStack.isEmpty()) {
                    // 清理当前线程变量。
                    contexts.remove();
                }
            }
        };
    }
}
