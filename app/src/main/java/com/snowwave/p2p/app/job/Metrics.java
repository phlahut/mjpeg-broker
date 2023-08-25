package com.snowwave.p2p.app.job;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Metrics {
    private double writtenFps;
    private String oneFpsWrittenCost;
    private final Map<String, Double> consumeFps;
    public Metrics(){
        consumeFps = new ConcurrentHashMap<>();
    }

    public double getWrittenFps() {
        return writtenFps;
    }

    public void setWrittenFps(double writtenFps) {
        this.writtenFps = writtenFps;
    }

    public String getOneFpsWrittenCost() {
        return oneFpsWrittenCost;
    }

    public void setOneFpsWrittenCost(String oneFpsWrittenCost) {
        this.oneFpsWrittenCost = oneFpsWrittenCost;
    }

    public void updateConsumeFps(String consumerId, double fps){
        if (consumeFps.containsKey(consumerId)){
            consumeFps.replace(consumerId, fps);
        } else {
            consumeFps.put(consumerId, fps);
        }
    }

    public void clear(){
        consumeFps.clear();
    }

    public Map<String, Double> getConsumeFps() {
        return consumeFps;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("writtenFps", writtenFps)
                .append("oneFpsWrittenCost", oneFpsWrittenCost)
                .append("consumeFps", consumeFps)
                .toString();
    }
}
