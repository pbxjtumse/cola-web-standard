package com.xjtu.iron.foundation.core.exception;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 提供面向日志和事件的堆栈文本生成能力。
 */
public final class StackTraceSupport {

    private StackTraceSupport() {
    }

    /**
     * 获取完整堆栈文本。
     */
    public static String asString(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        StringWriter buffer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(buffer));
        return buffer.toString();
    }

    /**
     * 获取限制长度的堆栈摘要。
     */
    public static String summarize(Throwable throwable, int maxCodePoints) {
        return com.xjtu.iron.foundation.core.text.TextTruncator.truncateWithSuffix(
                asString(throwable), maxCodePoints, "..."
        );
    }
}
