package com.xjtu.iron.cola.web.execution.impl.produce;

import com.xjtu.iron.cola.web.Message;
import com.xjtu.iron.cola.web.execution.SendExecution;
import com.xjtu.iron.cola.web.result.SendResult;

/**
 * 永不 UNCERTAIN
 * 永不 FAIL（除非 DB 挂）
 * 成功语义 = 已进入可补偿系统
 * BackgroundSendExecution 返回 SUCCESS（已进入可补偿系统）
 * @author pangbo
 * @date 2025/12/17
 */
public class BackgroundSendExecution implements SendExecution {

    private final OutboxRepository outboxRepository;

    /**
     * @param message
     * @return {@link SendResult }
     */
    @Override
    public SendResult execute(Message<?> message) {
        outboxRepository.save(message);
        return SendResult.success();
    }
}
