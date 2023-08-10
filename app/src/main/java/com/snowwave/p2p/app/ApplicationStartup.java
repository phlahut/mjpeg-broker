package com.snowwave.p2p.app;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.snowwave.p2p.component.transport.server.NettyServer;
import com.snowwave.p2p.component.transport.server.NettyWebServer;
import com.xuelang.mqstream.config.EnvUtil;
import com.xuelang.mqstream.handler.annotation.BussinessListener;
import com.xuelang.mqstream.message.MessageRecvService;
import com.xuelang.mqstream.message.arguments.MessageDataType;
import io.vertx.core.http.HttpServer;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${web.server.port}")
    private String port;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private NettyServer nettyServer;
    @Autowired
    private NettyWebServer nettyWebServer;
    @Autowired
    private HttpServer vertxHttpServer;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        String loglevel = EnvUtil.get("loglevel");
        if (StringUtils.hasText(loglevel)) {
            Level level = Level.toLevel(loglevel);
            refreshLoglevel(level);
        }

        Thread nettyExecutor = new Thread(() -> nettyServer.startServer());
        nettyExecutor.setName("netty-server-bootstrap");
        nettyExecutor.start();

        //Thread webExecutor = new Thread(() -> nettyWebServer.startHttpServer());
        Thread webExecutor = new Thread(() -> vertxHttpServer.listen(Integer.parseInt(port), "0.0.0.0", res -> {
            if (res.succeeded()) {
                log.info("vert.x success to listen:{}", port);
            } else {
                log.error("vert.x server start failed, {}", res.cause().getMessage());
            }
        }));

        webExecutor.setName("web-server-bootstrap");
        webExecutor.start();

        log.info("start recv suanpan msg");
        Map<String, Object> map = applicationContext.getBeansWithAnnotation(BussinessListener.class);
        Object[] bussiness = map.values().toArray();

        // 触发接收消息
        new MessageRecvService(Arrays.asList(bussiness), MessageDataType.COMMON.getCls()).subscribeMsg();
    }

    private void refreshLoglevel(Level loglevel){
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        List<Logger> loggerList = loggerContext.getLoggerList();
        // #2.filter the Logger object
        List<Logger> packageLoggerList = loggerList.stream().filter(a -> a.getName().startsWith("com.snowwave.p2p")).collect(Collectors.toList());
        // #3.set level to logger
        for (Logger logger : packageLoggerList) {
            logger.setLevel(loglevel);
        }
    }
}
