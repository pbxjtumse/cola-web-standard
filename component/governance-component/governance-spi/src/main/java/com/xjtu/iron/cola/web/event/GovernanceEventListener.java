package com.xjtu.iron.cola.web.event;

import com.xjtu.iron.cola.web.model.event.GovernanceEvent;

public interface GovernanceEventListener {

    boolean supports(GovernanceEvent event);

    void onEvent(GovernanceEvent event);
}
