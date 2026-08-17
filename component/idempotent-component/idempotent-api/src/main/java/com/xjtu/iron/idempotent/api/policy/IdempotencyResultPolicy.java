package com.xjtu.iron.idempotent.api.policy;

/**
 * SUCCESS 之后是否保存业务结果，以及重复请求命中 SUCCESS 时如何回放。
 *
 * <p>幂等组件最核心的职责是“重复请求不能重复执行业务”。这件事只依赖状态机，
 * 不依赖是否保存 callback 返回值。ResultPolicy 只决定用户体验层面的“第二次请求能不能拿到
 * 第一次的返回值快照”。</p>
 */
public enum IdempotencyResultPolicy {

    /**
     * 只保存 SUCCESS 状态，不保存业务返回值。
     *
     * <p>重复请求命中 SUCCESS 时返回 {@code REPLAYED}，但 {@code value} 为空。
     * 调用方只能知道“历史上已经成功”，不能直接拿到第一次的响应体。</p>
     */
    STATUS_ONLY(false),

    /**
     * SUCCESS 时保存 callback 返回值的序列化快照，后续重复请求直接反序列化并回放。
     *
     * <p>启用该策略时必须提供 {@code IdempotencyResultCodec}，并在 execute/recover 时传入
     * {@code resultType}。它适合“接口重复提交后必须返回同一个响应体”的场景。</p>
     */
    STORE_AND_REPLAY(true);

    private final boolean storesResult;

    IdempotencyResultPolicy(boolean storesResult) {
        this.storesResult = storesResult;
    }

    /** 是否需要在 SUCCESS 记录中保存 resultPayload。 */
    public boolean storesResult() {
        return storesResult;
    }
}
