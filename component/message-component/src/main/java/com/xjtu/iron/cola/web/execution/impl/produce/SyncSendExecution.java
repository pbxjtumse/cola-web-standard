package com.xjtu.iron.cola.web.execution.impl.produce;

import com.xjtu.iron.cola.web.Message;
import com.xjtu.iron.cola.web.client.MqProducerClient;
import com.xjtu.iron.cola.web.enums.SendFailTypeEnum;
import com.xjtu.iron.cola.web.exception.*;
import com.xjtu.iron.cola.web.execution.SendExecution;
import com.xjtu.iron.cola.web.result.SendResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * SyncSendExecution 返回 SUCCESS / FAIL / UNCERTAIN
 * @author pangbo
 * @date 2025/12/17
 */
@Component
@Qualifier("syncSendExecution")
public class SyncSendExecution implements SendExecution {

    private final MqProducerClient client;

    public SyncSendExecution(MqProducerClient client) {
        this.client = client;
    }

    @Override
    public SendResult execute(Message<?> message) {
        try {
            client.send(message);
            return SendResult.success();
        } catch (MqSerializationException e) {
            return SendResult.fail(SendFailTypeEnum.CLIENT_FAIL, e);
        } catch (MqAuthorizationException | MqBrokerRejectException e) {
            return SendResult.fail(SendFailTypeEnum.BROKER_REJECT, e);
        } catch (MqTimeoutException | MqNetworkException | MqUnknownException e) {
            return SendResult.uncertain(e);
        } catch (MqException e) {
            // 理论兜底
            return SendResult.uncertain(e);
        }
    }
}

