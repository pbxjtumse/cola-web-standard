package com.xjtu.iron.foundation.id.snowflake;

/** Snowflake 发现系统时钟回拨时采用的处理策略。 */
public enum ClockRollbackStrategy {

    /** 立即失败，避免隐藏基础设施时钟异常。 */
    FAIL_FAST,

    /** 在允许阈值内继续沿用上一次逻辑时间。 */
    USE_LOGICAL_TIME
}
