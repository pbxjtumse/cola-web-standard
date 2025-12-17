package com.xjtu.iron.cola.web.execution.impl.produce;

import com.xjtu.iron.cola.web.Message;
import com.xjtu.iron.cola.web.client.MqProducerClient;
import com.xjtu.iron.cola.web.execution.SendExecution;
import com.xjtu.iron.cola.web.result.SendResult;

/**
 * SyncSendExecution 返回 SUCCESS / FAIL / UNCERTAIN
 * @author pangbo
 * @date 2025/12/17
 */
public class SyncSendExecution implements SendExecution {

    /**
     *
     */
    private final MqProducerClient client;

    /**
     * @param message
     * @return {@link SendResult }
     */
    @Override
    public SendResult execute(Message<?> message) {

        client.send(message);

    }
}
