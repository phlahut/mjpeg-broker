package com.snowwave.p2p.app.web;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Author 胖还亮
 * @Date 2023/4/10 15:53
 * @Version 1.0
 */
@Component
public abstract class BaseVerticle extends AbstractVerticle {
    @Autowired
    public Router router;
}

