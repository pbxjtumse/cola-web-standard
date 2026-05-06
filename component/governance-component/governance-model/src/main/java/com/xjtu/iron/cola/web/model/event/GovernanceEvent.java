package com.xjtu.iron.cola.web.model.event;


import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GovernanceEvent {

    private String eventId = UUID.randomUUID().toString();

    private GovernanceEventType eventType;

    private String resourceName;

    private String traceId;

    private Instant occurredAt = Instant.now();

    private Map<String, Object> attributes = new HashMap<>();

    public static GovernanceEvent of(GovernanceEventType type, String resourceName) {
        GovernanceEvent event = new GovernanceEvent();
        event.setEventType(type);
        event.setResourceName(resourceName);
        return event;
    }

    public String getEventId() {
        return eventId;
    }

    public GovernanceEventType getEventType() {
        return eventType;
    }

    public void setEventType(GovernanceEventType eventType) {
        this.eventType = eventType;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
