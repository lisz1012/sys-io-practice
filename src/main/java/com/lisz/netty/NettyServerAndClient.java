package com.lisz.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Netty Server: start and connect this machine from: another host with:
 *    `nc 192.168.1.102 9090`
 * Netty Client: start nc with:
 *    `nc -l 192.168.1.99 9090`
 *  and connect to 192.168.1.99 with the program,and send message from server。
 */
public class NettyServerAndClient {

	@Test
	public void nettyServer() throws Exception {
		// IO 线程, 这里没有设置boss NioEventloopGroup, 一个线程做所有的事情，这是个测试中的特例
		NioEventLoopGroup group = new NioEventLoopGroup(1);
		ServerBootstrap serverBootstrap = new ServerBootstrap();
		ChannelFuture bind = serverBootstrap.group(group, group)
				.channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<NioSocketChannel>() {
					@Override
					protected void initChannel(NioSocketChannel ch) throws Exception {
						ch.pipeline().addLast(new MyInHandler());
					}
				})
				.bind("192.168.1.102", 9090);
		bind.sync().channel().closeFuture().sync();
	}



	@Test
	public void nettyClient() throws Exception {
		NioEventLoopGroup group = new NioEventLoopGroup(1);
		Bootstrap bootstrap = new Bootstrap();
		ChannelFuture connect = bootstrap
				.group(group)
				.channel(NioSocketChannel.class)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new MyInHandler());
					}
				})
				.connect(new InetSocketAddress("192.168.1.99", 9090));
		Channel client = connect.sync().channel();

		ByteBuf buf = Unpooled.copiedBuffer("Hello server.".getBytes());
		ChannelFuture send = client.writeAndFlush(buf);
		send.sync();

		client.closeFuture().sync();
	}
}
