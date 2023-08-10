package com.snowwave.p2p.component.transport.decoder;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.snowwave.p2p.common.buffer.BufferManager;
import com.snowwave.p2p.common.buffer.Producer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author 胖还亮
 * @Date 2022/8/5 14:11
 * @Version 1.0
 */
@Slf4j
public class FrameMsgDecoderPlus extends ChannelInboundHandlerAdapter {
    private Producer producer;
    private ByteBuf RECV_POOL;
    private static final byte JPEG_MARKER_PREFIX = -1; //FF
    private static final byte JPEG_START_MARKER = -40; //D8
    private static final byte JPEG_END_MARKER = -39;//D9
    private static final byte JPEG_COMPRESS_DATA_MARKER = -38;//DA
    private static final int JPEG_COMPRESS_DATA_GENERAL_SIZE = 80000;
    private boolean isExtractCompressGeneralData = false;
    private boolean isGotTopic = false;
    private final boolean isStartAsyncRead = false;
    private byte lastReadByte = -2;
    private Long topic;
    private JpegMarkType jpegMarkType;
    private boolean isExtractBlockSize = false;
    private boolean isExtractBlockMarker = false;
    private int blockSize = 0;
    private ByteBuf jpegBuf;
    private final ThreadPoolExecutor singlePoolExecutor;

    public FrameMsgDecoderPlus() {
        jpegMarkType = JpegMarkType.SEARCH_START;
        singlePoolExecutor = new ThreadPoolExecutor(1, 1, 0,
                TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(1),
                new ThreadFactoryBuilder().setNameFormat("single-thread-pool-%d").build(),
                new ThreadPoolExecutor.DiscardPolicy());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        RECV_POOL = PooledByteBufAllocator.DEFAULT.directBuffer();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        RECV_POOL.writeBytes((ByteBuf) msg);
        ((ByteBuf) msg).resetReaderIndex();
        ((ByteBuf) msg).resetWriterIndex();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ctx.pipeline().remove("ffmpeg_decoder");
        ctx.fireChannelInactive();
        ReferenceCountUtil.release(RECV_POOL);
        BufferManager.release(topic.toString());
        log.info("channel closed");
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        if (!isGotTopic && RECV_POOL.readableBytes() >= Long.BYTES) {
            topic = RECV_POOL.readLongLE();
            log.info("recv topic: {}", topic);
            isGotTopic = true;
            producer = BufferManager.createProducer(topic.toString());
        }

        singlePoolExecutor.execute(() -> {
            while (RECV_POOL.readableBytes() > 0) {
                switch (jpegMarkType) {
                    case END:
                        readComplete();
                        break;
                    case MARKER_BLOCKS:
                        readBlocks();
                        break;
                    case SEARCH_START:
                        searchStartMarker();
                        break;
                    case COMPRESS_DATA_BLOCK:
                        readCompressDataBlock();
                        break;
                    default:
                }
            }
        });
    }

    private void readCompressDataBlock() {
        if (!isExtractCompressGeneralData && RECV_POOL.readableBytes() >= JPEG_COMPRESS_DATA_GENERAL_SIZE) {
            jpegBuf.writeBytes(RECV_POOL, JPEG_COMPRESS_DATA_GENERAL_SIZE);
            isExtractCompressGeneralData = true;
        }

        if (isExtractCompressGeneralData && RECV_POOL.readableBytes() > 0) {
            searchEndMarker();
        }
    }

    private void readBlocks() {
        if (!isExtractBlockMarker && RECV_POOL.readableBytes() >= 2) {
            byte fir = RECV_POOL.readByte();
            byte sec = RECV_POOL.readByte();
            jpegBuf.writeByte(fir);
            jpegBuf.writeByte(sec);
            if (fir == JPEG_MARKER_PREFIX && sec == JPEG_COMPRESS_DATA_MARKER) {
                jpegMarkType = JpegMarkType.COMPRESS_DATA_BLOCK;
                isExtractBlockSize = false;
                isExtractBlockMarker = false;
                lastReadByte = -2;
                return;
            }

            isExtractBlockMarker = true;
        } else if (!isExtractBlockSize && RECV_POOL.readableBytes() >= 2) {
            byte fir = RECV_POOL.readByte();
            byte sec = RECV_POOL.readByte();
            jpegBuf.writeByte(fir);
            jpegBuf.writeByte(sec);
            blockSize = new BigInteger(new byte[]{fir, sec}).intValue() - 2;
            isExtractBlockSize = true;
        } else if (isExtractBlockMarker && isExtractBlockSize && RECV_POOL.readableBytes() >= blockSize) {
            jpegBuf.writeBytes(RECV_POOL, blockSize);
            isExtractBlockSize = false;
            isExtractBlockMarker = false;
        }
    }

    private void readComplete() {
        producer.publish();
        RECV_POOL.discardReadBytes();
        jpegMarkType = JpegMarkType.SEARCH_START;
        isExtractCompressGeneralData = false;
        isExtractBlockSize = false;
        isExtractBlockMarker = false;
        blockSize = 0;
        lastReadByte = -2;
    }

    private void searchStartMarker() {
        byte currentReadByte = RECV_POOL.readByte();
        if (JPEG_MARKER_PREFIX == lastReadByte && JPEG_START_MARKER == currentReadByte) {
            jpegBuf = producer.next();
            jpegBuf.writeByte(lastReadByte);
            jpegBuf.writeByte(currentReadByte);
            jpegMarkType = JpegMarkType.MARKER_BLOCKS;
            lastReadByte = -2;
        } else {
            lastReadByte = currentReadByte;
        }
    }

    private void searchEndMarker() {
        byte currentReadByte = RECV_POOL.readByte();
        if (JPEG_MARKER_PREFIX == lastReadByte && JPEG_END_MARKER == currentReadByte) {
            jpegBuf.writeByte(currentReadByte);
            jpegMarkType = JpegMarkType.END;
        } else {
            lastReadByte = currentReadByte;
        }
    }
}

enum JpegMarkType {
    COMPRESS_DATA_BLOCK,
    MARKER_BLOCKS,
    SEARCH_START,
    END
}
