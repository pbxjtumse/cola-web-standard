package com.xjtu.iron.cola.web.execution.impl.produce;

import com.xjtu.iron.cola.web.Message;
import com.xjtu.iron.cola.web.client.MqProducerClient;
import com.xjtu.iron.cola.web.execution.SendExecution;
import com.xjtu.iron.cola.web.result.SendResult;


/**
 * AsyncSendExecution 通常返回 SUCCESS（已受理）后续再通过异步记录补充事实
 * @author pangbo
 * @date 2025/12/17
 */
public class AsyncSendExecution implements SendExecution {

    /**
     *
     */
    private final MqProducerClient mqClient;

    /**
     * @param message
     * @return {@link SendResult }
     */
    @Override
    public SendResult execute(Message<?> message) {
        try {
            mqClient.sendAsync(message, new MqSendCallback() {
                @Override
                public void onSuccess(...) {
                    // 记录成功（通常异步）
                }

                @Override
                public void onException(Throwable t) {
                    // 记录 UNCERTAIN / FAIL
                }
            });
            return SendResult.accepted(); // 已受理
        } catch (Exception e) {
            return SendResult.fail(e);
        }
    }
}

