package com.snowwave.p2p.common.buffer;



/**
 * @Author 胖还亮
 * @Date 2023/3/19 22:01
 * @Version 1.0
 */
public interface Observable {
    void registerObserver(String id, Observer o);
    void removeObserver(String id);
    void asyncNotifyObservers(long index);
    void notifyObservers(long index);
}
