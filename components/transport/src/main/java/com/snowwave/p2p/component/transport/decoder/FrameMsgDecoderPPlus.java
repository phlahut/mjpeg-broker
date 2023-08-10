package com.snowwave.p2p.component.transport.decoder;

import com.snowwave.p2p.common.buffer.BufferManager;
import com.snowwave.p2p.common.buffer.Producer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author 胖还亮
 * @Date 2022/8/5 14:11
 * @Version 1.0
 */
@Slf4j
public class FrameMsgDecoderPPlus extends ChannelInboundHandlerAdapter {
    private Producer producer;
    private ByteBuf JPEG_CACHE;
    private static final byte JPEG_START_END_PREFIX = -1; //FF
    private static final byte JPEG_START = -40; //D8
    private static final byte JPEG_END = -39;//D9

    private byte lastReadByte = -2;
    private boolean BODY_START = false;
    private boolean GOT_HEADER = false;
    private Long topic;

    private ByteBuf byteBuf;

    public FrameMsgDecoderPPlus() {
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        JPEG_CACHE = PooledByteBufAllocator.DEFAULT.directBuffer();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            JPEG_CACHE.writeBytes((ByteBuf) msg);
        } finally {
            ReferenceCountUtil.release(msg);
        }

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ctx.pipeline().remove("ffmpeg_decoder");
        ctx.fireChannelInactive();
        ReferenceCountUtil.release(JPEG_CACHE);
        BufferManager.release(topic.toString());
        log.info("channel closed, topic:{}", topic);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("channel exception close it, exception: {}", cause.getLocalizedMessage());
        ctx.pipeline().remove("ffmpeg_decoder");
        ctx.fireChannelInactive();
        ReferenceCountUtil.release(JPEG_CACHE);
        BufferManager.release(topic.toString());
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        while (JPEG_CACHE.readableBytes() > 0) {
            if (!GOT_HEADER && JPEG_CACHE.readableBytes() >= Long.BYTES) {
                topic = JPEG_CACHE.readLongLE();
                log.info("recv topic: {}", topic);
                GOT_HEADER = true;
                producer = BufferManager.createProducer(topic.toString());
                continue;
            }


            byte currentReadByte = JPEG_CACHE.readByte();
            //jpeg开头
            if (!BODY_START && lastReadByte == JPEG_START_END_PREFIX && currentReadByte == JPEG_START) {
                byteBuf = producer.next();
                byteBuf.writeByte(lastReadByte);
                BODY_START = true;
            } else if (BODY_START && lastReadByte == JPEG_START_END_PREFIX && currentReadByte == JPEG_END) {
                byteBuf.writeByte(lastReadByte);
                byteBuf.writeByte(currentReadByte);
                producer.publish();
                lastReadByte = -2;
                BODY_START = false;
                JPEG_CACHE.discardReadBytes();
            }

            if (BODY_START) {
                byteBuf.writeByte(currentReadByte);
            }

            lastReadByte = currentReadByte;
        }
    }
}
