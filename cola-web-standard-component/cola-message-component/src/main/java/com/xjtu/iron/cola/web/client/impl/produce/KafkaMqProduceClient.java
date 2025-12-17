package com.xjtu.iron.cola.web.client.impl.produce;

import com.xjtu.iron.cola.web.Message;
import com.xjtu.iron.cola.web.client.MqProducerClient;
import com.xjtu.iron.cola.web.client.MqSendCallback;
import com.xjtu.iron.cola.web.exception.*;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.*;




public class KafkaMqProduceClient implements MqProducerClient {

    private final KafkaProducer<String, byte[]> producer;
    private final TopicResolver topicResolver;

    @Override
    public void send(Message<?> message) {
        try {
            ProducerRecord<String, byte[]> record = buildRecord(message);
            producer.send(record).get();
        }
        catch (SerializationException e) {
            throw new MqSerializationException(e);
        }
        catch (AuthorizationException e) {
            throw new MqAuthorizationException(e);
        }
        catch (TimeoutException e) {
            throw new MqTimeoutException(e);
        }
        catch (NetworkException e) {
            throw new MqNetworkException(e);
        }
        catch (Exception e) {
            throw new MqUnknownException(e);
        }
    }

    @Override
    public void sendAsync(Message<?> message, MqSendCallback callback) {

    }
}

