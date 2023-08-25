package com.snowwave.p2p.app.job;


import com.alibaba.fastjson.JSON;
import com.snowwave.p2p.common.buffer.BufferManager;
import com.snowwave.p2p.common.buffer.Consumer;
import com.snowwave.p2p.common.buffer.Producer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class StatisticsJob {
    private final Map<String, Long> writtenFpsRecord = new ConcurrentHashMap<>();
    private final Map<String, Long> consumeFpsRecord = new ConcurrentHashMap<>();
    private final NumberFormat nf = NumberFormat.getNumberInstance();
    private Map<String, Metrics> statisticsMetrics = new ConcurrentHashMap<>();
    private static final long cycle = 10000;

    @Scheduled(fixedRate = cycle)
    public void doStatistic() {
        nf.setMaximumFractionDigits(1);
        Map<String, Producer> producerMap = BufferManager.getProducerMap();
        Set<String> topics = producerMap.keySet();
        for (String topic : topics) {
            Metrics metrics = statisticsMetrics.computeIfAbsent(topic, k -> new Metrics());
            Producer producer = producerMap.get(topic);
            long curCursorIndex = producer.getCursorIndex();
            if (writtenFpsRecord.containsKey(topic)) {
                long lastCursorIndex = writtenFpsRecord.get(topic);
                writtenFpsRecord.replace(topic, curCursorIndex);
                double writtenFps = (double) (curCursorIndex - lastCursorIndex) * 1000 / cycle;
                double costMillis = 1000 / writtenFps;
                producer.updateWrittenCost(costMillis);
                metrics.setWrittenFps(writtenFps);
                metrics.setOneFpsWrittenCost(nf.format(costMillis));
            } else {
                writtenFpsRecord.put(topic, curCursorIndex);
                metrics.setWrittenFps(0);
                metrics.setOneFpsWrittenCost(nf.format(0));
            }

            Map<String, Consumer> consumerMap = BufferManager.getConsumers(topic);
            if (consumerMap != null) {
                for (String consumerId : consumerMap.keySet()) {
                    Consumer consumer = consumerMap.get(consumerId);
                    long curReadIndex = consumer.getReadCursor();
                    if (consumeFpsRecord.containsKey(consumerId)) {
                        long lastReadIndex = consumeFpsRecord.get(consumerId);
                        double consumeFps = (double) (curReadIndex - lastReadIndex) * 1000 / cycle;
                        metrics.updateConsumeFps(consumerId, consumeFps);
                        consumeFpsRecord.replace(consumerId, curReadIndex);
                        if (consumeFps <= 0) {
                            BufferManager.removeConsumer(consumer.getTopic(), consumer.getConsumerId());
                            consumeFpsRecord.remove(consumerId);
                        }
                    } else {
                        consumeFpsRecord.put(consumerId, curReadIndex);
                    }
                }
            }
        }

        log.info("broker info: {}", JSON.toJSONString(statisticsMetrics));
    }
}
