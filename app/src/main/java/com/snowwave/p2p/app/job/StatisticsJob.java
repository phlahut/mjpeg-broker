package com.snowwave.p2p.app.job;


import com.snowwave.p2p.common.buffer.BufferManager;
import com.snowwave.p2p.common.buffer.Consumer;
import com.snowwave.p2p.common.buffer.Producer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class StatisticsJob {
    private final Map<String, Long> topicWrittenFpsMap = new ConcurrentHashMap<>();
    private final Map<String, Long> topicConsumeFpsMap = new ConcurrentHashMap<>();
    private final NumberFormat nf = NumberFormat.getNumberInstance();
    private final StringBuilder sb = new StringBuilder();
    private static final long cycle = 10000;

    @Scheduled(fixedRate = cycle)
    private void doStatistic() {
        nf.setMaximumFractionDigits(1);
        Map<String, Producer> producerMap = BufferManager.getProducerMap();
        Set<String> topics = producerMap.keySet();
        for (String topic : topics) {
            Producer producer = producerMap.get(topic);
            long curCursorIndex = producer.getCursorIndex();
            if (topicWrittenFpsMap.containsKey(topic)) {
                long lastCursorIndex = topicWrittenFpsMap.get(topic);
                topicWrittenFpsMap.replace(topic, curCursorIndex);
                double writtenFps = (double) (curCursorIndex - lastCursorIndex) * 1000 / cycle;
                double costMillis = 1000 / writtenFps;
                producer.updateWrittenCost(costMillis);
                sb.append("{ ").append("topic: ").append(topic).append(", ").append("writtenFps: ").append(nf.format(writtenFps))
                        .append(", ").append("oneFpsCost: ").append(nf.format(costMillis)).append("} ");
            } else {
                topicWrittenFpsMap.put(topic, curCursorIndex);
            }
        }

        Map<String, Consumer> consumerMap = BufferManager.getConsumerMap();
        Set<String> consumerKeys = consumerMap.keySet();
        for (String key : consumerKeys) {
            long curReadIndex = consumerMap.get(key).getReadCursor();
            if (topicConsumeFpsMap.containsKey(key)) {
                long lastReadIndex = topicConsumeFpsMap.get(key);
                topicConsumeFpsMap.replace(key, curReadIndex);
                sb.append("{ ").append("consumerKey: ").append(key).append(", ").append("consumeFps: ")
                        .append(nf.format((double) (curReadIndex - lastReadIndex) * 1000 / cycle)).append("} ");
            } else {
                topicConsumeFpsMap.put(key, curReadIndex);
            }
        }

        log.info("broker fps:{}", sb);
        sb.delete(0, sb.length());
    }
}
