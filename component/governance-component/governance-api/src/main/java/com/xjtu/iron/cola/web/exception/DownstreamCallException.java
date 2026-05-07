package com.xjtu.iron.cola.web.exception;

public class DownstreamCallException extends GovernanceException {

    public DownstreamCallException(String resourceName, Throwable cause) {
        super("Downstream call failed: " + resourceName,
                resourceName,
                "GOV-500-001",
                true,
                cause);
    }
}
