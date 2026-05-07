package com.xjtu.iron.cola.web.exception;

public class DownstreamRateLimitedException extends GovernanceException {

    public DownstreamRateLimitedException(String resourceName, Throwable cause) {
        super("Rate limited: " + resourceName,
                resourceName,
                "GOV-429-001",
                false,
                cause);
    }
}
