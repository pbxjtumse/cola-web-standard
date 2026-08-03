package com.xjtu.iron.retry.config.properties;

import com.xjtu.iron.retry.api.policy.OperationSafety;
import com.xjtu.iron.retry.api.policy.RetryFailureCategory;
import com.xjtu.iron.retry.api.policy.RetrySafetyMode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** 保存完成继承合并和默认值填充后的内部策略配置。 */
final class ResolvedRetryPolicyProperties {

    /** 默认最大尝试次数。 */
    int maxAttempts = 3;
    /** 默认最大总时长预算。 */
    Duration maxDuration = Duration.ofSeconds(5);
    /** 默认未声明操作安全性。 */
    OperationSafety operationSafety = OperationSafety.UNSPECIFIED;
    /** 默认对不安全重试发出告警。 */
    RetrySafetyMode safetyMode = RetrySafetyMode.WARN;
    /** 默认不遍历异常 cause 链。 */
    boolean traverseCauses;
    /** 默认最多遍历十六层 cause。 */
    int maxCauseDepth = 16;
    /** 配置 retry-on 时使用的统一失败分类。 */
    RetryFailureCategory retryFailureCategory = RetryFailureCategory.TRANSIENT;
    /** 配置 retry-on 时使用的稳定失败码。 */
    String retryFailureCode = "CONFIGURED_RETRYABLE_EXCEPTION";
    /** 已解析的可重试异常类名。 */
    List<String> retryOn = new ArrayList<>();
    /** 已解析的正常停止异常类名。 */
    List<String> stopOn = new ArrayList<>();
    /** 已解析的立即中止异常类名。 */
    List<String> abortOn = new ArrayList<>();
    /** 已解析的退避类型。 */
    RetryProperties.BackoffType backoffType = RetryProperties.BackoffType.NONE;
    /** 已解析的固定等待时长。 */
    Duration delay = Duration.ZERO;
    /** 已解析的指数退避初始时长。 */
    Duration initialDelay = Duration.ofMillis(100);
    /** 已解析的指数退避最大时长。 */
    Duration maxDelay = Duration.ofSeconds(1);
    /** 已解析的指数增长倍数。 */
    double multiplier = 2.0D;

    /** 创建一个完全独立的深复制对象。 */
    ResolvedRetryPolicyProperties copy() {
        ResolvedRetryPolicyProperties copy = new ResolvedRetryPolicyProperties();
        copy.maxAttempts = maxAttempts;
        copy.maxDuration = maxDuration;
        copy.operationSafety = operationSafety;
        copy.safetyMode = safetyMode;
        copy.traverseCauses = traverseCauses;
        copy.maxCauseDepth = maxCauseDepth;
        copy.retryFailureCategory = retryFailureCategory;
        copy.retryFailureCode = retryFailureCode;
        copy.retryOn = new ArrayList<>(retryOn);
        copy.stopOn = new ArrayList<>(stopOn);
        copy.abortOn = new ArrayList<>(abortOn);
        copy.backoffType = backoffType;
        copy.delay = delay;
        copy.initialDelay = initialDelay;
        copy.maxDelay = maxDelay;
        copy.multiplier = multiplier;
        return copy;
    }
}
