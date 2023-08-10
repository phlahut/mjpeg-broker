package com.snowwave.p2p.common.buffer;

import com.snowwave.p2p.common.pool.ThreadPoolFactory;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Author 胖还亮
 * @Date 2023/3/17 16:15
 * @Version 1.0
 */
@Slf4j
public class RingBuffer implements Observable {
    private final Entry[] entries;
    private final String topic;
    private final int size;
    private volatile long writeCursor;
    private volatile double averageWrittenCost = 50;
    private final Map<String, Observer> consumerGroup = new ConcurrentHashMap<>();

    public RingBuffer(String topic, Integer size) {
        this.topic = topic;
        this.size = size;
        this.entries = new Entry[size];
        fill(this.entries);
    }

    public void updateWrittenCost(double cost){
        this.averageWrittenCost = cost;
        log.debug("topic:{} one fps written cost:{} millis", topic, cost);
    }

    public double getAverageWrittenCost(){
        return averageWrittenCost;
    }

    public String getTopic(){
        return topic;
    }

    public int size(){
        return size;
    }

    public long getWriteCursor(){
        return writeCursor;
    }

    public ByteBuf nextWrite(long index){
        int ringPoint = (int) (index % size);
        ByteBuf item = entries[ringPoint].getData();
        item.setZero(0,item.capacity());
        item.clear();
        writeCursor = index;
        return item;
    }


    public ByteBuf read(int ringPoint) {
        return entries[ringPoint].getData();
    }

    private void fill(Entry[] entries) {
        for (int i = 0; i < entries.length; i++) {
            entries[i] = new Entry();
        }
    }


    @Override
    public void registerObserver(String id, Observer o) {
        if (!consumerGroup.containsKey(id)) {
            consumerGroup.put(id, o);
        } else {
            consumerGroup.replace(id, o);
        }
    }

    @Override
    public void removeObserver(String id) {
        if (!consumerGroup.containsKey(id)) {
            consumerGroup.remove(id);
        }
    }

    @Override
    public void asyncNotifyObservers(long index) {
        if (!consumerGroup.isEmpty()) {
            ThreadPoolExecutor poolExecutor = ThreadPoolFactory.getThreadPool();
            consumerGroup.values().forEach(observer -> poolExecutor.execute(() -> observer.update(index)));
        }
    }

    @Override
    public void notifyObservers(long index) {
        if (!consumerGroup.isEmpty()) {
            consumerGroup.values().parallelStream().forEach(observer -> observer.update(index));
        }
    }

    public void release() {
        for (Entry entry: entries){
            entry.getData().release(entry.getData().refCnt());
        }

        consumerGroup.clear();
    }
}
