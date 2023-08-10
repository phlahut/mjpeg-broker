package com.snowwave.p2p.component.transport.server;

import com.alibaba.fastjson.JSONObject;
import com.snowwave.p2p.common.utils.SnowResponseUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CorsHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Author 胖还亮
 * @Date 2023/4/10 15:42
 * @Version 1.0
 */
@Slf4j
@Configuration
public class VertxServer {

    @Bean
    public Vertx vertx() {
        VertxOptions options = new VertxOptions()
                .setEventLoopPoolSize(Runtime.getRuntime().availableProcessors() * 2)
                .setWorkerPoolSize(100);
        return Vertx.vertx(options);
    }

    @Bean
    public Router router(Vertx vertx) {
        Router router = Router.router(vertx);
        globalIntercept(router);
        globalError(router);
        cors(router);
        return router;
    }

    @Bean
    public HttpServer httpServer(Vertx vertx, Router router, ApplicationContext applicationContext) {
        //获取所有子类
        Map<String, AbstractVerticle> beansOfType = applicationContext.getBeansOfType(AbstractVerticle.class);
        beansOfType.forEach((k, verticle) -> vertx.deployVerticle(verticle));

        HttpServerOptions options = new HttpServerOptions();
        options.setIdleTimeoutUnit(TimeUnit.MILLISECONDS);
        options.setTcpKeepAlive(true);
        options.setReusePort(true);
        options.setIdleTimeout(8000);
        options.setWriteIdleTimeout(8000);
        options.setReadIdleTimeout(8000);
        return vertx.createHttpServer(options).requestHandler(router);
    }

    //跨域处理
    private void cors(Router router) {
        router.route().handler(CorsHandler.create()
                .addOrigin("*")
                .allowedHeader(" x-www-form-urlencoded, Content-Type,x-requested-with")
                .allowedMethod(HttpMethod.GET)
                .allowedMethod(HttpMethod.POST)
                .allowedMethod(HttpMethod.PUT)
                .allowedMethod(HttpMethod.DELETE));

    }

    //全局异常返回
    private void globalError(Router router) {
        router.route().failureHandler(ctx -> ctx.response().setStatusCode(500).end(JSONObject.toJSONString(
                SnowResponseUtil.buildResponse(500, ctx.failure().getMessage(), null))));
    }

    //全局拦截器
    private void globalIntercept(Router router) {
        router.route("/*").handler(ctx -> {
            ctx.response().putHeader("Content-Type", "application/json");
            ctx.next();
        });
    }
}
