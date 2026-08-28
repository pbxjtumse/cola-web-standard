package com.xjtu.iron.message.core.consume;

import com.xjtu.iron.message.api.consume.ConsumeContext;
import com.xjtu.iron.message.api.consume.ConsumeDecision;

/**
 * v13 默认异常分类器：所有业务异常都保守转 RETRY。
 */
public final class DefaultConsumeExceptionClassifier implements ConsumeExceptionClassifier {
    @Override
    public ConsumeDecision classify(Throwable throwable, ConsumeContext context) {
        return ConsumeDecision.RETRY;
    }
}
