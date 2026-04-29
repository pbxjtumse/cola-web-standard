package com.xjtu.iron.cola.web.exception;

/**
 * 不是线程池拒绝，是系统治理主动拒绝
 * @author pangbo
 * @date 2025/12/19
 */
public class GovernanceRejectedException extends RuntimeException {

    /**
     * @param message
     */
    public GovernanceRejectedException(String message) {
        super(message);
    }
}
