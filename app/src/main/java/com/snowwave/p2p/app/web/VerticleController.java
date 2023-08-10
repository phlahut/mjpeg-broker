package com.snowwave.p2p.app.web;

import com.alibaba.fastjson.JSONObject;
import com.snowwave.p2p.common.buffer.BufferManager;
import com.snowwave.p2p.common.buffer.Consumer;
import com.snowwave.p2p.common.utils.SnowResponseUtil;
import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.locks.LockSupport;

/**
 * @Author 胖还亮
 * @Date 2023/4/10 15:54
 * @Version 1.0
 */
@Slf4j
@Component
public class VerticleController extends BaseVerticle {

    private final ThreadLocal<Buffer> THREAD_LOCAL = new ThreadLocal<>();

    private final long oneMillis = 1000000;

    @Override
    public void start() {
        //blockingHandler的ordered必须设置成false,避免不同消费者之间消费速度不一致互相影响
        router.get("/api/vision/image/get").blockingHandler(this::consume, false);
        router.get("/err").handler(this::err);
    }

    private void consume(RoutingContext ctx) {
        String topic = ctx.queryParams().get("topic");
        String consumerId = ctx.queryParams().get("consumerId");
        String fps = ctx.queryParams().get("fps");
        if (!StringUtils.hasText(topic) || !StringUtils.hasText(consumerId) || !StringUtils.hasText(fps)) {
            ctx.response().setStatusCode(400).end(JSONObject.toJSONString(SnowResponseUtil.buildResponse(
                    400, "invalid params", null)));
            return;
        }

        Consumer consumer = BufferManager.createOrGetConsumer(topic, consumerId, Integer.parseInt(fps));
        if (consumer == null) {
            LockSupport.parkNanos(oneMillis * 1000);
            ctx.response().setStatusCode(400).end(JSONObject.toJSONString(SnowResponseUtil.buildResponse(
                    400, "invalid params", null)));
            return;
        }

        ByteBuf data = consumer.read();
        if (data != null) {
            data.retain();
            Buffer response = THREAD_LOCAL.get();
            if (response == null) {
                response = MyBufferImpl.buffer(data);
                THREAD_LOCAL.set(response);
            }

            ((MyBufferImpl) response).setBuffer(data);
            ctx.response()
                    .putHeader("content-type", "image/jpeg")
                    .putHeader(HttpHeaders.TRANSFER_ENCODING, "chunked")
                    .putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(data.readableBytes()))
                    .end(response, null);
        } else {
            ctx.response().setStatusCode(400).end(JSONObject.toJSONString(SnowResponseUtil.buildResponse(
                    400, "get image failed", null)));
        }
    }

    private void err(RoutingContext ctx) {
        try {
            throw new Exception("Internal Server Error");
        } catch (Exception e) {
            // 处理异常并返回合适的响应
            ctx.response().setStatusCode(500).end("Internal Server Error");
            // 或者记录异常堆栈信息
            log.error("An error occurred:", e);
        }
    }
}
