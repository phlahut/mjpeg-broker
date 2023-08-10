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
    private static final ConcurrentHashMap<String, Object> producerLocks = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> TOPIC_REL_CONSUMERKEYS = new ConcurrentHashMap<>();
    private static final Map<String, Consumer> CONSUMER_MAP = new ConcurrentHashMap<>();

    public static Map<String, Producer> getProducerMap(){
        return PRODUCER_MAP;
    }

    public static Map<String, Consumer> getConsumerMap(){
        return CONSUMER_MAP;
    }

    public static Producer createProducer(String topic) {
        producerLocks.putIfAbsent(topic, new Object());
        synchronized (producerLocks.get(topic)){
            if (!PRODUCER_MAP.containsKey(topic)) {
                PRODUCER_MAP.put(topic, new Producer(new RingBuffer(topic, Constants.RING_BUFFER_SIZE)));
                TOPIC_REL_CONSUMERKEYS.putIfAbsent(topic, new ArrayList<>());
            }
        }

        return PRODUCER_MAP.get(topic);
    }

    public static Consumer createOrGetConsumer(String topic, String consumerId, int fps) {
        if (!PRODUCER_MAP.containsKey(topic)) {
            log.debug("topic has no producer, topic:{}", topic);
            return null;
        }

        String consumerKey = topic + "_" + consumerId;
        if (CONSUMER_MAP.containsKey(consumerKey)) {
            return CONSUMER_MAP.get(consumerKey);
        }

        RingBuffer buffer = PRODUCER_MAP.get(topic).getRingBuffer();
        Consumer newConsumer  = new Consumer(consumerId, buffer, fps);
        Consumer previousConsumer = CONSUMER_MAP.putIfAbsent(consumerKey, newConsumer );
        if (previousConsumer == null){
            buffer.registerObserver(consumerId, newConsumer);
        } else {
            newConsumer = previousConsumer;
        }

        TOPIC_REL_CONSUMERKEYS.computeIfAbsent(topic, k -> new ArrayList<>()).add(consumerKey);
        return newConsumer;
    }

    public static void removeConsumer(String topic, String consumerId) {
        String consumerKey = topic + "_" + consumerId;
        if (CONSUMER_MAP.containsKey(consumerKey)) {
            CONSUMER_MAP.remove(consumerKey);
            TOPIC_REL_CONSUMERKEYS.get(topic).remove(consumerKey);
        }
    }

    public static void release(String topic) {
        if (PRODUCER_MAP.containsKey(topic)) {
            PRODUCER_MAP.get(topic).release();
            PRODUCER_MAP.remove(topic);
            List<String> consumers = TOPIC_REL_CONSUMERKEYS.get(topic);
            if (!CollectionUtils.isEmpty(consumers)) {
                for (String consumerKey: consumers){
                    CONSUMER_MAP.get(consumerKey).release();
                    CONSUMER_MAP.remove(consumerKey);
                }
            }

            TOPIC_REL_CONSUMERKEYS.remove(topic);
        }
    }

    public static Collection<Producer> getProducers() {
        return PRODUCER_MAP.values();
    }
}
