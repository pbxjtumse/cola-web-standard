package com.xjtu.iron.cola.web;

public interface Bulkhead {

    boolean tryAcquire();

    void release();

    int getLimit();

    int getInUse();

    void updateLimit(int newLimit);
}
