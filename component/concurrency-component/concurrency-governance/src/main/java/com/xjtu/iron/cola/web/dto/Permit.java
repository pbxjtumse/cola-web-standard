package com.xjtu.iron.cola.web.dto;

import java.util.Collections;
import java.util.List;

/**
 * 资源借条 + 归还清单
 * @author pbxjt
 * @date 2025/12/19
 */
public  class Permit {

    /**
     *
     */
    private final boolean acquired;
    /**
     *
     */
    private final List<Runnable> releaseActions;

    /**
     * @param acquired
     * @param releaseActions
     */
    private Permit(boolean acquired, List<Runnable> releaseActions) {
        this.acquired = acquired;
        this.releaseActions = releaseActions;
    }

    /**
     * @return {@link Permit }
     */
    public static Permit rejected() {
        return new Permit(false, Collections.<Runnable>emptyList());
    }

    /**
     * @param releaseActions
     * @return {@link Permit }
     */
    public static Permit acquired(List<Runnable> releaseActions) {
        return new Permit(true, releaseActions);
    }

    /**
     * @return boolean
     */
    public boolean isAcquired() {
        return acquired;
    }

    /**
     *
     */
    public void release() {
        for (Runnable r : releaseActions) {
            try {
                r.run();
            } catch (Throwable t) {
                // log but never break
            }
        }
    }
}

