package com.snowwave.p2p.component.transport.server;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.snowwave.p2p.component.transport.decoder.FrameMsgDecoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollMode;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioChannelOption;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.RejectedExecutionHandlers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.StandardSocketOptions;
import java.util.concurrent.TimeUnit;


/**
 * @Author 胖还亮
 * @Date 2022/8/5 14:03
 * @Version 1.0
 */
@Slf4j
@Service
public class NettyServer {
    @Value("${p2p.server.port}")
    private String port;
    private static final DefaultEventExecutorGroup handlerExecutorGroup = new DefaultEventExecutorGroup(100,
            new ThreadFactoryBuilder().setNameFormat("netty-handler-pool-%d").build(), 2048, RejectedExecutionHandlers.reject());


    public void startServer() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(),
                new ThreadFactoryBuilder().setNameFormat("netty-server-boss-%d").build());
        EventLoopGroup workerGroup = new NioEventLoopGroup(100,
                new ThreadFactoryBuilder().setNameFormat("netty-server-worker-%d").build());

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128) //初始化服务端可连接队列,指定了队列的大小128
                .option(EpollChannelOption.SO_REUSEPORT, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT.directBuffer().alloc())
                .childOption(ChannelOption.SO_KEEPALIVE, true) //保持长连接
                .childOption(NioChannelOption.of(StandardSocketOptions.SO_KEEPALIVE), true)
                .childOption(ChannelOption.ALLOCATOR,PooledByteBufAllocator.DEFAULT.directBuffer().alloc())
                .childHandler(new ChannelInitializer<SocketChannel>() {  // 绑定客户端连接时候触发操作
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        log.info("channel connected, client addr:{}", channel.remoteAddress().toString());
                        channel.pipeline().addLast(new IdleStateHandler(10, 0, 0, TimeUnit.SECONDS));
                        channel.pipeline().addLast(/*handlerExecutorGroup,*/ "ffmpeg_decoder", new FrameMsgDecoder());
                    }
                });

        if (Epoll.isAvailable()) {
            bootstrap.option(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED)
                    .option(EpollChannelOption.TCP_QUICKACK, Boolean.TRUE);
        }
        //绑定监听端口，调用sync同步阻塞方法等待绑定操作完
        ChannelFuture future;
        try {
            future = bootstrap.bind(Integer.parseInt(port)).sync();
            log.info("netty server start success, port:{}", port);
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("start netty server error " + e.getMessage(), e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
