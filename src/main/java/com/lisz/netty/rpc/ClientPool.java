package com.lisz.netty.rpc;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.util.Random;

// 封装了一个Channel数组和怎么填这个数组
public class ClientPool {
	private NioSocketChannel[] clients;
	private Object[] lock;
	private Random rand = new Random();
	private int size;
	private InetSocketAddress address;
	private NioEventLoopGroup clientWorker;

	public ClientPool(int size, InetSocketAddress address) {
		this.size = size;
		this.address = address;
		clients = new NioSocketChannel[size];
		lock = new Object[size]; // 锁是可以初始化的
		for (int i = 0; i < size; i++) {
			lock[i] = new Object();
		}
	}

	public synchronized NioSocketChannel getNioSocketChannel() {
		final int i = rand.nextInt(size);
		if (clients[i] != null && clients[i].isActive()) {
			return clients[i];
		}
		clients[i] = create(address);
		return clients[i];
	}

	private NioSocketChannel create(InetSocketAddress address) {
		clientWorker = new NioEventLoopGroup(10);
		Bootstrap bs = new Bootstrap();
		final ChannelFuture connect = bs.group(clientWorker)
				.channel(NioSocketChannel.class)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new ServerDecoder()).addLast(new ClientResponse()); //解决给谁的
					}
				})
				.connect(address);
		try {
			final Channel client = connect.sync().channel();
			return (NioSocketChannel) client;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}
}
