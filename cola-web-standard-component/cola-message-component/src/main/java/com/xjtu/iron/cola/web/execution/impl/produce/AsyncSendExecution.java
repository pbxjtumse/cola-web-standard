package com.xjtu.iron.cola.web.execution.impl.produce;

import com.xjtu.iron.cola.web.AsyncSendRecorder;
import com.xjtu.iron.cola.web.Message;
import com.xjtu.iron.cola.web.client.MqProducerClient;
import com.xjtu.iron.cola.web.client.MqSendCallback;
import com.xjtu.iron.cola.web.enums.SendFailTypeEnum;
import com.xjtu.iron.cola.web.exception.MqAuthorizationException;
import com.xjtu.iron.cola.web.exception.MqBrokerRejectException;
import com.xjtu.iron.cola.web.exception.MqSerializationException;
import com.xjtu.iron.cola.web.execution.SendExecution;
import com.xjtu.iron.cola.web.result.SendResult;


/**
 * AsyncSendExecution 通常返回 SUCCESS（已受理）后续再通过异步记录补充事实
 * @author pangbo
 * @date 2025/12/17
 */

public class AsyncSendExecution implements SendExecution {

    private final MqProducerClient mqClient;
    private final AsyncSendRecorder recorder;

    /**
     * @param message
     * @return {@link SendResult }
     */
    @Override
    public SendResult execute(Message<?> message) {
        try {
            mqClient.sendAsync(message, new MqSendCallback() {
                @Override
                public void onSuccess() {
                    recorder.onSuccess(message);
                }

                @Override
                public void onFailure(Throwable ex) {
                    SendResult result = mapException(ex);
                    recorder.onFailure(message, result);
                }
            });
            return SendResult.success();
        } catch (Exception e) {
            return mapException(e);
        }
    }

    private SendResult mapException(Throwable ex) {
        if (ex instanceof MqSerializationException) {
            return SendResult.fail(SendFailTypeEnum.CLIENT_FAIL, ex);
        }
        if (ex instanceof MqAuthorizationException
                || ex instanceof MqBrokerRejectException) {
            return SendResult.fail(SendFailTypeEnum.BROKER_REJECT, ex);
        }
        return SendResult.uncertain(ex);
    }
}

