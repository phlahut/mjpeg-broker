package com.snowwave.p2p.common.buffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * @Author 胖还亮
 * @Date 2023/3/13 17:10
 * @Version 1.0
 */
public class Entry {
    private ByteBuf data;

    public Entry(){
        data = Unpooled.directBuffer(204800);
    }

    public ByteBuf getData() {
        return data;
    }

    public void setData(ByteBuf data) {
        this.data = data;
    }
}
