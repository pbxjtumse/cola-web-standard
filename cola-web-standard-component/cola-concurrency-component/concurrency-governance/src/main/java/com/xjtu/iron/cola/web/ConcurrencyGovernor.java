package com.xjtu.iron.cola.web;

public interface ConcurrencyGovernor {

    void execute(String poolName, Runnable task);

}

