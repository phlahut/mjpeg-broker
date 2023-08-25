package com.snowwave.p2p.common.buffer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author 胖还亮
 * @Date 2022/8/12 17:03
 * @Version 1.0
 */
@Slf4j
public class BufferManager {
    private static final Map<String, Producer> PRODUCER_MAP = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Object> producerLocks = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Consumer>> TOPIC_CONSUMERS_MAP = new ConcurrentHashMap<>();

    public static Map<String, Producer> getProducerMap() {
        return PRODUCER_MAP;
    }


    public static Consumer getConsumer(String topic, String consumerId) {
        Map<String, Consumer> consumerMap = TOPIC_CONSUMERS_MAP.get(topic);
        if (consumerMap != null) {
            return consumerMap.get(consumerId);
        }
        return null;
    }

    public static Map<String, Consumer> getConsumers(String topic) {
        return TOPIC_CONSUMERS_MAP.get(topic);
    }


    public static Producer createProducer(String topic) {
        return PRODUCER_MAP.computeIfAbsent(topic, k -> {
            Producer producer = new Producer(new RingBuffer(topic, Constants.RING_BUFFER_SIZE));
            return producer;
        });
    }



    public static Consumer createOrGetConsumer(String topic, String consumerId, int fps) {
        if (!PRODUCER_MAP.containsKey(topic)) {
            log.debug("topic has no producer, cannot create consumer, topic:{}", topic);
            return null;
        }

        Map<String, Consumer> consumerMap = TOPIC_CONSUMERS_MAP.computeIfAbsent(topic, k -> new ConcurrentHashMap<>());
        return consumerMap.computeIfAbsent(consumerId, s -> {
            RingBuffer buffer = PRODUCER_MAP.get(topic).getRingBuffer();
            Consumer newConsumer = new Consumer(consumerId, buffer, fps);
            buffer.registerObserver(consumerId, newConsumer);
            return newConsumer;
        });
    }



    public static void removeConsumer(String topic, String consumerId) {
        Map consumerMap = TOPIC_CONSUMERS_MAP.get(topic);
        if (consumerMap != null) {
            consumerMap.remove(consumerId);
            RingBuffer buffer = PRODUCER_MAP.get(topic).getRingBuffer();
            buffer.removeObserver(consumerId);
            log.info("topic:{} remove idle consumer:{}", topic, consumerId);
        }
    }

    /*public static void release(String topic) {
        if (PRODUCER_MAP.containsKey(topic)) {
            PRODUCER_MAP.get(topic).release();
            PRODUCER_MAP.remove(topic);
            List<String> consumers = TOPIC_REL_CONSUMERKEYS.get(topic);
            if (!CollectionUtils.isEmpty(consumers)) {
                for (String consumerKey : consumers) {
                    CONSUMER_MAP.get(consumerKey).release();
                    CONSUMER_MAP.remove(consumerKey);
                }
            }

            TOPIC_REL_CONSUMERKEYS.remove(topic);
        }
    }*/

    public static void release(String topic) {
        if (PRODUCER_MAP.containsKey(topic)) {
            PRODUCER_MAP.get(topic).release();
            PRODUCER_MAP.remove(topic);

            Map<String, Consumer> consumerMap = TOPIC_CONSUMERS_MAP.get(topic);
            if (consumerMap != null) {
                consumerMap.clear();
                TOPIC_CONSUMERS_MAP.remove(topic);
            }
        }
    }

    public static Collection<Producer> getProducers() {
        return PRODUCER_MAP.values();
    }
}
