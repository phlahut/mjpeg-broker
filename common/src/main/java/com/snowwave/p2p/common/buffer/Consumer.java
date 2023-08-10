package com.snowwave.p2p.common.buffer;

import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * @Author 胖还亮
 * @Date 2023/3/14 10:04
 * @Version 1.0
 */
@Slf4j
public class Consumer implements Observer {
    private final String consumerId;
    private final RingBuffer ringBuffer;
    private final AtomicLong readCursor;

    /**
     * 每个下标对应的state 0不可读 value=1可读，
     */
    private Sequence[] readControlRing;
    private final Thread[] parkRing;
    private final Sequence readControlCursor;
    private final int[] readIntervalRing;
    private Sequence readIntervalCursor;
    private final int bufferSize;
    private static final long oneMillis = 1000000;
    private static final long maxCost = 50;
    private static final long averageCost = 45;
    private final long oneCost = averageCost * oneMillis;
    private long lastWriteCursor = 0;

    public Consumer(String consumerId, RingBuffer ringBuffer, int fps) {
        this.consumerId = consumerId;
        this.ringBuffer = ringBuffer;
        this.readControlCursor = new Sequence(ringBuffer.getWriteCursor() + 1);
        this.readIntervalCursor = new Sequence();
        this.bufferSize = ringBuffer.size();
        this.readControlRing = new Sequence[bufferSize];
        fill(readControlRing);
        this.readIntervalRing = IndexSliceUtils.calculateIntervals(bufferSize, fps);
        this.parkRing = new Thread[this.bufferSize];
        this.readCursor = new AtomicLong(0L);
    }

    public long getReadCursor() {
        return readCursor.get();
    }

    public ByteBuf read() {
        int interval = nextInterval();
        long index = nextIndex(interval);
        int point = (int) (index % readControlRing.length);
        //log.debug("topic:{} consumer:{} begin read point:{}, index:{} interval:{}", ringBuffer.getTopic(), consumerId, point, index, interval);
        return casOrParkRead(point, interval);
    }

    @Override
    public void update(long index) {
        setReadAvailable((int) (index % bufferSize));
    }


    private ByteBuf casOrParkRead(int point, int interval) {
        int casMaxTimes = 0;
        long initialParkTime = averageCost * interval * oneMillis;
        while (true) {
            if (casMaxTimes <= 3) {
                if (readControlRing[point].compareAndSet(1, 0)) {
                    return doRead(point);
                }

                casMaxTimes++;
                int gap = interval - (int) (ringBuffer.getWriteCursor() - lastWriteCursor);
                long parkTime;
                if (gap > 0){
                    parkTime = Math.min((long) (ringBuffer.getAverageWrittenCost() * gap * oneMillis), initialParkTime);
                } else{
                    parkTime = oneCost;
                }

                LockSupport.parkNanos(parkTime);
                log.debug("topic:{}, consumer:{}, read point:{}, jump gap:{}, cas count:{}, parkTime:{}", ringBuffer.getTopic(), consumerId,
                        point, gap, casMaxTimes, parkTime);
                lastWriteCursor = ringBuffer.getWriteCursor();
            } else {
                return doLongParkRead(point, interval);
            }
        }
    }

    private ByteBuf doLongParkRead(int point, int interval) {
        longPark(point, interval);
        if (readControlRing[point].compareAndSet(1, 0)) {
            return doRead(point);
        } else {
            log.debug("topic:{} consumer:{} read failed point:{}", ringBuffer.getTopic(), consumerId, point);
            return null;
        }
    }

    private void longPark(int point, int interval) {
        parkRing[point] = Thread.currentThread();
        long parkTime = oneMillis * maxCost * interval;
        long start = System.currentTimeMillis();
        LockSupport.parkNanos(parkTime);
        parkRing[point] = null;
        log.debug("topic:{} consumer:{} park at point:{} cost:{} millis ", ringBuffer.getTopic(), consumerId, point, System.currentTimeMillis() - start);
    }

    private ByteBuf doRead(int point) {
        log.debug("topic:{} consumer:{} read success point:{}", ringBuffer.getTopic(), consumerId, point);
        ByteBuf buf = ringBuffer.read(point);
        readControlRing[point].setVolatile(0);
        readCursor.incrementAndGet();
        return buf;
    }


    private void fill(Sequence[] state) {
        for (int i = 0; i < state.length; i++) {
            state[i] = new Sequence(0);
        }
    }

    private int nextInterval() {
        long index = readIntervalCursor.incrementAndGet();
        int ringPoint = (int) (index % readIntervalRing.length);
        return readIntervalRing[ringPoint];
    }

    private long nextIndex(int interval) {
        return readControlCursor.addAndGet(interval);
    }

    private long nextIndex() {
        return readControlCursor.incrementAndGet();
    }


    private void setReadAvailable(int point) {
        readControlRing[point].setVolatile(1);
        if (parkRing[point] != null) {
            LockSupport.unpark(parkRing[point]);
            parkRing[point] = null;
            log.debug("topic:{} consumer:{} read unpark point:{}", ringBuffer.getTopic(), consumerId, point);
        }
    }

    public void release() {
        if (parkRing != null && parkRing.length > 0) {
            for (Thread t : parkRing) {
                LockSupport.unpark(t);
            }
        }

        if (readControlRing != null){
            readControlRing = null;
        }

        readIntervalCursor = null;
    }
}

