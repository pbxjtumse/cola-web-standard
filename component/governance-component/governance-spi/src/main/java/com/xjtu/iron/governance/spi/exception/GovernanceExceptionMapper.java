package com.xjtu.iron.governance.spi.exception;

import com.xjtu.iron.governance.api.context.GovernanceContext;

public interface GovernanceExceptionMapper {

    boolean supports(Throwable throwable);

    RuntimeException map(GovernanceContext context, Throwable throwable);
}
