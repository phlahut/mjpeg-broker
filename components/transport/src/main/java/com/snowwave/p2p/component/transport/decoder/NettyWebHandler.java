package com.snowwave.p2p.component.transport.decoder;

import com.snowwave.p2p.common.buffer.BufferManager;
import com.snowwave.p2p.common.buffer.Consumer;
import com.snowwave.p2p.component.transport.encoder.ZeroCPHttpResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.concurrent.locks.LockSupport;

@Slf4j
public class NettyWebHandler extends ChannelInboundHandlerAdapter {
    private static final String endpoint = "/api/vision/image/get";
    private static final long oneMills = 1000000;
    private final ThreadLocal<ZeroCPHttpResponse> THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        //log.info("http client:{} connected", ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        FullHttpRequest httpRequest;
        if (msg instanceof FullHttpRequest) {
            httpRequest = (FullHttpRequest) msg;
            String uri = httpRequest.uri();
            if (!uri.startsWith(endpoint)) {
                LockSupport.parkNanos(3000 * oneMills);
                sendError(ctx, HttpResponseStatus.NOT_FOUND, "endpoint not found");
                return;
            }

            if (uri.startsWith("/favicon.ico")) {
                LockSupport.parkNanos(3000 * oneMills);
                sendError(ctx, HttpResponseStatus.BAD_REQUEST, "invalid request");
                return;
            }


            String topic = null;
            String consumerId = null;
            int fps = 0;
            try {
                String tmp = uri.split("\\?")[1];
                String[] tmp1 = tmp.split("\\&");
                for (int i = 0; i < tmp1.length; i++) {
                    if (tmp1[i].startsWith("topic")) {
                        topic = tmp1[i].split("\\=")[1];
                    } else if (tmp1[i].startsWith("consumerId")) {
                        consumerId = tmp1[i].split("\\=")[1];
                    } else {
                        fps = Integer.parseInt(tmp1[i].split("\\=")[1]);
                    }
                }
            } catch (Exception e) {
                LockSupport.parkNanos(3000 * oneMills);
                sendError(ctx, HttpResponseStatus.BAD_REQUEST, "invalid params");
                return;
            }

            if (!StringUtils.hasText(topic) || !StringUtils.hasText(consumerId) || fps == 0) {
                LockSupport.parkNanos(3000 * oneMills);
                sendError(ctx, HttpResponseStatus.BAD_REQUEST, "invalid params");
                return;
            }

            Consumer consumer = BufferManager.createOrGetConsumer(topic, consumerId, fps);
            if (consumer == null) {
                LockSupport.parkNanos(3000 * oneMills);
                sendError(ctx, HttpResponseStatus.NOT_FOUND, "topic not found");
                return;
            }

            ByteBuf data = consumer.read();
            if (data != null) {
                data.asReadOnly();
                ZeroCPHttpResponse response = THREAD_LOCAL.get();
                if (response == null) {
                    response = new ZeroCPHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                    THREAD_LOCAL.set(response);
                }

                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "image/jpeg");
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                response.setProtocolVersion(HttpVersion.HTTP_1_1);
                response.setStatus(HttpResponseStatus.OK);
                response.setContent(data);
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, data.readableBytes());
                response.retain();
                ctx.writeAndFlush(response);
            } else {
                sendError(ctx, HttpResponseStatus.BAD_REQUEST, "get image failed!");
            }
        }

        ReferenceCountUtil.release(msg);
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, String msg) {
        ZeroCPHttpResponse response = THREAD_LOCAL.get();
        if (response == null) {
            response = new ZeroCPHttpResponse(HttpVersion.HTTP_1_1, status);
            THREAD_LOCAL.set(response);
        }

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        ByteBuf msgBuf = Unpooled.wrappedBuffer(msg.getBytes());
        response.setContent(msgBuf);
        response.setStatus(status);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, msgBuf.readableBytes());
        response.retain();
        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        sendError(ctx, HttpResponseStatus.BAD_REQUEST, cause.getMessage());
        ctx.close();
    }
}
