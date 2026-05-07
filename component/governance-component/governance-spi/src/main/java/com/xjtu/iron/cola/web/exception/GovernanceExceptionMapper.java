package com.xjtu.iron.cola.web.exception;

import com.xjtu.iron.cola.web.context.GovernanceContext;

public interface GovernanceExceptionMapper {

    boolean supports(Throwable throwable);

    RuntimeException map(GovernanceContext context, Throwable throwable);
}
