package com.snowwave.p2p.common.buffer;

import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author 胖还亮
 * @Date 2022/8/12 14:02
 * @Version 1.0
 */
@Slf4j
public class Producer {
    private Sequence cursor;
    private final RingBuffer ringBuffer;
    private long lastTimestamp;



    public void updateWrittenCost(double cost){
        ringBuffer.updateWrittenCost(cost);
    }

    public double getAverageWrittenCost(){
        return ringBuffer.getAverageWrittenCost();
    }


    public Producer(RingBuffer ringBuffer) {
        this.cursor = new Sequence();
        this.ringBuffer = ringBuffer;
    }

    public long getCursorIndex(){
        return cursor.get();
    }

    public void publish() {
        long index = cursor.get();
        ringBuffer.notifyObservers(cursor.get());
        log.debug("topic:{} written index:{} cost:{} millis", ringBuffer.getTopic(), index, cost());
    }

    private long cost() {
        long currTimestamp = System.currentTimeMillis();
        long cost = currTimestamp - lastTimestamp;
        lastTimestamp = currTimestamp;
        return cost;
    }

    public ByteBuf next() {
        long index = cursor.incrementAndGet();
        ByteBuf slot = ringBuffer.nextWrite(index);
        slot.ensureWritable(0, true);
        return slot;
    }


    public RingBuffer getRingBuffer() {
        return ringBuffer;
    }

    public void release() {
        ringBuffer.release();
        cursor = null;
    }
}
