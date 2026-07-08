package com.xjtu.iron.distributed.lock.api;


import java.util.Optional;

public interface DistributedLockClient {

    LockResult<LockHandle> tryLock(String lockName, LockOptions options);

    <T> LockResult<T> execute(
            String lockName,
            LockOptions options,
            LockCallback<T> callback
    );

    boolean isHeld(LockHandle handle);

    boolean unlock(LockHandle handle);

    boolean renew(LockHandle handle);
}
