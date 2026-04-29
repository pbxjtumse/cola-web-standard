package com.xjtu.iron.cola.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "xy.observability")
public class ObservabilityProperties {

    private boolean enabled = true;

    private String instrumentationName = "xy-observability";

    private boolean traceAspectEnabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getInstrumentationName() {
        return instrumentationName;
    }

    public void setInstrumentationName(String instrumentationName) {
        this.instrumentationName = instrumentationName;
    }

    public boolean isTraceAspectEnabled() {
        return traceAspectEnabled;
    }

    public void setTraceAspectEnabled(boolean traceAspectEnabled) {
        this.traceAspectEnabled = traceAspectEnabled;
    }
}
