package com.xjtu.iron.governance.spi.event;

import com.xjtu.iron.governance.model.event.GovernanceEvent;

public interface GovernanceEventListener {

    boolean supports(GovernanceEvent event);

    void onEvent(GovernanceEvent event);
}
