package com.snowwave.p2p.common.buffer;

/**
 * @Author 胖还亮
 * @Date 2023/3/20 10:19
 * @Version 1.0
 */
public class IndexSliceUtils {

    public static int[] calculateIntervals(int len, int num) {
        int[] positions = new int[num];
        int[] delta = new int[num];
        int interval = len / num;
        int remainder = len % num;
        int pos = interval / 2;
        for (int i = 0; i < num; i++) {
            positions[i] = pos;
            int extra = i < remainder ? 1 : 0;
            pos += interval + extra;
        }

        for (int i = 0; i < num-1; i++){
            delta[i] =  positions[i+1] -positions[i];
        }

        delta[num-1] = len - positions[num-1] + positions[0];

        return delta;
    }
}
