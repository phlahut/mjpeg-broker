package com.snowwave.p2p.component.transport.decoder;

import com.snowwave.p2p.common.buffer.BufferManager;
import com.snowwave.p2p.common.buffer.Producer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @Author 胖还亮
 * @Date 2022/8/5 14:11
 * @Version 1.0
 */
@Slf4j
public class FrameMsgDecoder extends ChannelInboundHandlerAdapter {
    private Producer producer;
    //private ByteBuf JPEG_CACHE;
    private static final byte JPEG_START_END_PREFIX = -1; //FF
    private static final byte JPEG_START = -40; //D8
    private static final byte JPEG_END = -39;//D9
    private byte lastReadByte = -2;
    private boolean BODY_START = false;
    private boolean GOT_TOPIC_CHAR_LEN = false;
    private int topicCharLen;
    private StringBuilder stringBuilder;
    private String topic;
    private ByteBuf slot;


    public FrameMsgDecoder() {
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        stringBuilder = new StringBuilder();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf msgBuf = (ByteBuf) msg;
        while (msgBuf.readableBytes() > 0) {
            //解析topic长度
            if (!GOT_TOPIC_CHAR_LEN) {
                if (msgBuf.readableBytes() < Integer.BYTES) {
                    continue;
                }

                topicCharLen = msgBuf.readIntLE();
                GOT_TOPIC_CHAR_LEN = true;
            }

            //解析topic
            if (topicCharLen > 0) {
                CharSequence sequence = msgBuf.readCharSequence(1, StandardCharsets.UTF_8);
                char readChar = sequence.charAt(0);
                stringBuilder.append(readChar);
                topicCharLen--;
                if (topicCharLen == 0) {
                    topic = stringBuilder.toString();
                    producer = BufferManager.createProducer(topic);
                    log.info("recv topic: {}", topic);
                }

                continue;
            } else if (topicCharLen < 0) {
                topicCharLen = 0;
                topic = "default";
                producer = BufferManager.createProducer(topic);
                log.info("recv topic null, use default");
            }

            //解析jpeg内容
            byte currentReadByte = msgBuf.readByte();
            //jpeg开头
            if (!BODY_START && lastReadByte == JPEG_START_END_PREFIX && currentReadByte == JPEG_START) {
                slot = producer.next();
                slot.writeByte(lastReadByte);
                BODY_START = true;
            } else if (BODY_START && lastReadByte == JPEG_START_END_PREFIX && currentReadByte == JPEG_END) {
                slot.writeByte(lastReadByte);
                slot.writeByte(currentReadByte);
                producer.publish();
                lastReadByte = -2;
                BODY_START = false;
            }

            if (BODY_START) {
                slot.writeByte(currentReadByte);
            }

            lastReadByte = currentReadByte;
        }

        ReferenceCountUtil.release(msgBuf);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ctx.pipeline().remove("ffmpeg_decoder");
        if (StringUtils.hasText(topic)){
            BufferManager.release(topic);
        }

        stringBuilder = null;
        log.info("channel closed, topic:{}", topic);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("channel exception close it, cause: {}", cause.getLocalizedMessage());
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {

    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                // 处理读取空闲状态事件
                log.info("topic:{} has been idle for a while, begin close connection", topic);
                ctx.close(); // 可以在这里关闭连接
            }
        }
    }
}
