package com.snowwave.p2p.component.transport.server;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.snowwave.p2p.component.transport.decoder.NettyWebHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollMode;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NettyWebServer {
    @Value("${web.server.port}")
    private String port;

    public void startHttpServer() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(),
                new ThreadFactoryBuilder().setNameFormat("netty-web-server-boss-%d").build());
        EventLoopGroup workerGroup = new NioEventLoopGroup(100,
                new ThreadFactoryBuilder().setNameFormat("netty-web-server-worker-%d").build());

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128) //初始化服务端可连接队列,指定了队列的大小128
                .option(EpollChannelOption.SO_REUSEPORT, true)
                //.option(ChannelOption.SO_SNDBUF, 2 * 1024 * 1024)  // 设置发送缓冲区大小为1MB
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_LINGER, 0)
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) {
                        //server端发送的是httpResponse，所以要使用HttpResponseEncoder进行编码
                        ch.pipeline().addLast(new HttpServerCodec());
                        ch.pipeline().addLast(new HttpObjectAggregator(10 * 1024 * 1024));
                        ch.pipeline().addLast(new NettyWebHandler());
                    }
                }).option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        if (Epoll.isAvailable()) {
            bootstrap.option(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED)
                    .option(EpollChannelOption.TCP_QUICKACK, Boolean.TRUE);
        }
        //绑定监听端口，调用sync同步阻塞方法等待绑定操作完
        ChannelFuture future;
        try {
            future = bootstrap.bind(Integer.parseInt(port)).sync();
            log.info("http server start success, port:{}", port);
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("start http server error " + e.getMessage(), e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
