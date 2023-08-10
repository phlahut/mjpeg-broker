package com.snowwave.p2p.common.pool;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolFactory {
    private static final ThreadFactory namedThreadPoolFactory = new ThreadFactoryBuilder().setNameFormat("unPark-notify-pool-%d").build();
    private static volatile ThreadPoolExecutor threadPoolExecutor = null;

    private static final int cpus = Runtime.getRuntime().availableProcessors();

    public static ThreadPoolExecutor getThreadPool() {
        if (threadPoolExecutor == null) {
            synchronized (namedThreadPoolFactory) {
                if (threadPoolExecutor != null){
                    return threadPoolExecutor;
                }

                threadPoolExecutor = new ThreadPoolExecutor(cpus, cpus*2, 0,
                        TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(1024), namedThreadPoolFactory,
                        new ThreadPoolExecutor.DiscardPolicy());
                return threadPoolExecutor;
            }
        }

        return threadPoolExecutor;
    }
}
