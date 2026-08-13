package com.xjtu.iron.transaction.api;

/**
 * 当前 execute 调用与物理事务之间的关系。
 */
public enum TransactionParticipation {

    /** 当前调用创建并拥有一个新的物理事务。 */
    OWNER,

    /** 当前调用只是加入已经存在的物理事务。 */
    PARTICIPANT
}
