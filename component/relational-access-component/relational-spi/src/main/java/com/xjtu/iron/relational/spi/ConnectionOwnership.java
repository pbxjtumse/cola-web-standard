package com.xjtu.iron.relational.spi;

/**
 * 当前 Relational Access 调用对物理 Connection 的所有权。
 */
public enum ConnectionOwnership {

    /** 本次访问自行获取 Connection，release 时应物理关闭。 */
    OWNED,

    /** Connection 由外部事务上下文持有，release 时只能逻辑释放，不能物理关闭。 */
    BORROWED
}
