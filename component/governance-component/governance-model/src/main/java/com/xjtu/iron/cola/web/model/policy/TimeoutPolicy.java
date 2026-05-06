package com.xjtu.iron.cola.web.model.policy;


import java.time.Duration;

public class TimeoutPolicy {

    private boolean enabled = true;

    /**
     * 一期作为治理层总等待时间。
     * 真实 HTTP 连接超时、读取超时仍然建议在 HTTP 客户端里配置。
     */
    private Duration timeout = Duration.ofSeconds(1);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }
}
