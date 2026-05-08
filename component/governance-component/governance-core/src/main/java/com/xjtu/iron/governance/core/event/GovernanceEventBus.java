package com.xjtu.iron.governance.core.event;

import com.xjtu.iron.governance.model.event.GovernanceEvent;

public interface GovernanceEventBus {

    void publish(GovernanceEvent event);
}
