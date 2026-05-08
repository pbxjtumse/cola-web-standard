package com.xjtu.iron.governance.api.context;

public final class GovernanceContextHolder {

    private static final ThreadLocal<GovernanceContext> HOLDER = new ThreadLocal<>();

    private GovernanceContextHolder() {
    }

    public static void set(GovernanceContext context) {
        HOLDER.set(context);
    }

    public static GovernanceContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}