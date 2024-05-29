package com.lisz.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Test;

import java.net.InetSocketAddress;

public class MyNetty {
	@Test
	public void myBytebuf() {
//		ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(8, 20);
		// pool 也可以用堆外内存（direct）
		//ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.heapBuffer(8, 20);
		ByteBuf buf = PooledByteBufAllocator.DEFAULT.heapBuffer(8, 20);
		print(buf);
		buf.writeBytes(new byte[]{1,2,3,4});
		print(buf);
		buf.writeBytes(new byte[]{1,2,3,4});
		print(buf);
		buf.writeBytes(new byte[]{1,2,3,4});
		print(buf);
		buf.writeBytes(new byte[]{1,2,3,4});
		print(buf);
		buf.writeBytes(new byte[]{1,2,3,4});
		print(buf);
//		buf.writeBytes(new byte[]{1,2,3,4}); // 报错，超过最大capacity.
//		print(buf);
	}



	private static void print(ByteBuf buf) {
		System.out.println("buf.isReadable() = " + buf.isReadable());           // 可不可读
		System.out.println("buf.readerIndex() = " + buf.readerIndex());         // 从哪里读
		System.out.println("buf.readableBytes() = " + buf.readableBytes());     // 可读字节数
		System.out.println("buf.isWritable() = " + buf.isWritable());           // 可不可写
		System.out.println("buf.writerIndex() = " + buf.writerIndex());         // 写位置
		System.out.println("buf.writableBytes() = " + buf.writableBytes());     // 可写字节数
		System.out.println("buf.capacity() = " + buf.capacity());               // 动态分配的，变化的
		System.out.println("buf.maxCapacity() = " + buf.maxCapacity());         // 设置好的
		System.out.println("buf.isDirect() = " + buf.isDirect());               // true为堆外内存
		System.out.println("---------------------------------------------");
	}



	/**
	 * 客户端
	 * 连接别人
	 * 1. 主动连接别人
	 * 2. 别人什么时候给我发？event selector
	 */
	@Test
	public void loopExecutor() throws Exception {
		// 先把group理解成一个线程池, 下面设置的是 2 个线程, 所以最多能 execute 两个任务
		NioEventLoopGroup selector = new NioEventLoopGroup(2);
		selector.execute(() -> {
			while (true) {
				System.out.println("Hello world 001!");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		});
		selector.execute(() -> {
			while (true) {
				System.out.println("Hello world 002");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		});
		System.in.read();
	}



	@Test
	public void clientMode() throws Exception {
		// Netty多路复用器
		NioEventLoopGroup thread = new NioEventLoopGroup();

		// 客户端模式
		NioSocketChannel client = new NioSocketChannel();

		// 读写都要用多路复用器：EventLoop. 客户端注册到多路复用器上
		// 否则报错：channel not registered to an event loop
		thread.register(client); // epoll_ctl(5, ADD, 3)

		// 响应式的，有了事件才会调用Handler
		ChannelPipeline pipeline = client.pipeline();
		pipeline.addLast(new MyInHandler());

		// reactor 异步的特征，在另一个线程里connect，所以要等Future的get
		ChannelFuture connect = client.connect(new InetSocketAddress("192.168.1.99", 9090));

		// 连上了才往下走, 等待异步处理的结果。
		ChannelFuture sync = connect.sync();

		ByteBuf buf = Unpooled.copiedBuffer("hello server".getBytes());
		ChannelFuture send = client.writeAndFlush(buf);

		// 发送成功才能往下走，这里又是异步！
		send.sync();

		// 会阻塞在关闭等待上，以上步骤可能来回多次，是个长连接，服务端退出的时候会往下走。
		sync.channel().closeFuture().sync();

		System.out.println("client over...");
	}



	// Server： nc -l 192.168.1.99 9090
	@Test
	public void nettyClient() throws Exception {
		NioEventLoopGroup group = new NioEventLoopGroup(1);
		Bootstrap bootstrap = new Bootstrap();
		ChannelFuture connect = bootstrap.group(group)
				.channel(NioSocketChannel.class)
				.handler(new ChannelInitializer<SocketChannel>() { // Channel（默认）也可以
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new MyInHandler());
					}
				}).connect(new InetSocketAddress("192.168.1.99", 9090));
		Channel client = connect.sync().channel();

		ByteBuf buf = Unpooled.copiedBuffer("Hello server.".getBytes());
		ChannelFuture send = client.writeAndFlush(buf);
		send.sync();

		client.closeFuture().sync();
	}



	@Test
	public void serverMode() throws Exception {
		NioEventLoopGroup thread = new NioEventLoopGroup(1);
		NioServerSocketChannel server = new NioServerSocketChannel();
		thread.register(server);
		// 指不定什么时候家里来人, 响应式，预埋，之后的某个时间执行。
		// 注册事件，观察者模式。提前写逻辑，被用到的时候才执行。
		// accept接收并且注册到selector
		server.pipeline().addLast(new MyAcceptHandler(thread, new ChannelInit(){
			@Override
			protected void initChannel(ChannelHandlerContext ctx) {
				Channel channel = ctx.channel();
				channel.pipeline().addLast(new MyInHandler());
				ctx.pipeline().remove(this);
				// 3 client::pipeliine[MyInHandler]
			}
		}));
		// 下面有坑：hostname写localhost的话，别的机器作为client nc连不上来
		ChannelFuture serverFuture = server.bind(new InetSocketAddress("192.168.1.102", 9090));
		serverFuture.sync().channel().closeFuture().sync();

		System.out.println("Server close ...");
	}



	// 自己摸索推导出来的，太高兴啦！
	@Test
	public void nettyServer() throws Exception {
		NioEventLoopGroup thread = new NioEventLoopGroup(1);
		ServerBootstrap serverBootstrap = new ServerBootstrap();
		ChannelFuture connect = serverBootstrap
				.group(thread, thread) // 一个是boss一个是worker，可复用同一个，所以调用一个参数的也可以
				.channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<NioSocketChannel>() {
					@Override
					protected void initChannel(NioSocketChannel ch) throws Exception { // SocketChannel也行
						ch.pipeline().addLast(new MyInHandler());
					}
				})
				.bind("192.168.1.102", 9090);
		connect.sync().channel().closeFuture().sync();
	}
}
