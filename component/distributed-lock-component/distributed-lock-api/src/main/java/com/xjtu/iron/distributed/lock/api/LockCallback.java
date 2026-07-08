package com.xjtu.iron.distributed.lock.api;

@FunctionalInterface
public interface LockCallback<T> {

    T doWithLock(LockHandle handle) throws Exception;
}
