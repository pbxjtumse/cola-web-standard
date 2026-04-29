package com.xjtu.iron.cola.web;

/**
 * 并发资源： 现在还能不能再多一个并发执行者？
 *  Bulkhead 是一个“可以借 / 可以还 / 有上限的资源池”。
 */
public interface Bulkhead {

    boolean tryAcquire();

    void release();

    int getLimit();

    int getInUse();

    void updateLimit(int newLimit);
}
