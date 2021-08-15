package com.lisz.netty.rpc;
/**
 * @author: 马士兵教育
 * @create: 2020-07-12 20:08
 *
 * 12号的课开始手写RPC ，把前边的IO的课程都看看
 *http://mashibing.com/vip.html#%E5%91%A8%E8%80%81%E5%B8%88%E5%86%85%E5%AD%98%E4%B8%8Eio%E7%A3%81%E7%9B%98io%E7%BD%91%E7%BB%9Cio
 */

/*
    1，先假设一个需求，写一个RPC
    2，来回通信，连接数量，拆包？
    3，动态代理呀，序列化，协议封装
    4，连接池
    5，RPC: 就像调用本地方法一样去调用远程的方法，面向java中就是所谓的 面向interface开发
      表面上是调用接口的方法，其实底层是代理类，实现了远程的调用
 */


import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * 上节课，基本写了一个能发送
 * 小问题，当并发通过一个连接发送后，服务端解析bytebuf 转 对象的过程出错
 */

public class MyRPCTest {


	// 模拟客户端
	@Test
	public void get() throws Exception {
		new Thread(()->{startServer();}).start();
		int size = 80;
		Thread[] threads = new Thread[size];
		for (int i = 0; i < size; i++) {
			threads[i] = new Thread(()->{
//				Fly fly = proxyGet(Fly.class);
//				fly.xxoo("hello");
				Car car = proxyGet(Car.class); //动态代理实现
				car.ooxx("hello");
			});
		}
		for (Thread thread : threads) {
			thread.start();
		}
		System.in.read();


//		Car car = proxyGet(Car.class); //动态代理实现
//		car.ooxx("hello");

	}

	private static <T>T proxyGet(Class<T> clazz) {
		//实现各个版本动态代理
		ClassLoader classLoader = clazz.getClassLoader();
		Class<?>[] intfces = {clazz};
		return (T) Proxy.newProxyInstance(classLoader, intfces, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				//如何设计我们的consumer对于provider的调用过程
				//1，调用 服务，方法，参数  ==> 封装成message  [content]  按理说还要有一个注册发现中心，这里先略
				// 把接口名和方法名打包成对象，然后序列化，然后发出去给服务端
				String name = intfces[0].getName();
				String methodName = method.getName();
				Class<?>[] parameterTypes = method.getParameterTypes();
				MyContent content = new MyContent();
				content.name = name;
				content.methodName = methodName;
				content.parameterTypes = parameterTypes;
				content.setArgs(args);

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(baos);
				oos.writeObject(content);
				byte[] msgBody = baos.toByteArray();

				//2，requestID+message  ，本地要缓存
				//协议：【header<>】【msgBody
				MyHeader header = createHeader(msgBody);
				baos.reset();
				oos = new ObjectOutputStream(baos);
				oos.writeObject(header);
				// TODO: Server: dispatcher Excecutor
				byte[] msgHeader = baos.toByteArray();

				ByteBuf buf = Unpooled.copiedBuffer(msgHeader, msgBody);


				//3，连接池：：取得连接
				ClientFactory factory = ClientFactory.getInstance();
				NioSocketChannel clientChannel
						= factory.getClient(new InetSocketAddress("192.168.1.102", 9090));
				//4，发送--> 走IO  out -->走Netty（event 驱动）
				final CountDownLatch latch = new CountDownLatch(1);
				ResponseHandler.addCallBack(header.getRequestId(), new MyCallBack(latch));
				final ChannelFuture send = clientChannel.writeAndFlush(buf);
				send.sync();



				latch.await();

				//5，？，如果从IO ，未来回来了，怎么将代码执行到这里
				//（睡眠/回调，如何让线程停下来？你还能让他继续。。。 Latch）
				return null;
			}
		});
	}

	private static MyHeader createHeader(byte[] msgBody) {
		final MyHeader myHeader = new MyHeader();
		int size = msgBody.length;
		int f = 0x14141414;
		long requestId = Math.abs(UUID.randomUUID().getLeastSignificantBits());
		myHeader.setFlag(f);
		myHeader.setDataLength(size);
		myHeader.setRequestId(requestId);
		return myHeader;
	}

	//Server端
	@Test
	public void startServer() {
		NioEventLoopGroup boss = new NioEventLoopGroup(1);
		NioEventLoopGroup workers = new NioEventLoopGroup(1);
		ServerBootstrap serverBootstrap = new ServerBootstrap();
		final ChannelFuture bind = serverBootstrap.group(boss, workers)
				.channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<NioSocketChannel>() {
					@Override
					protected void initChannel(NioSocketChannel ch) throws Exception {
						System.out.println("Server accept client port: " + ch.remoteAddress().getPort());
						ch.pipeline().addLast(new RequestHandler());
					}
				})
				.bind("192.168.1.102", 9090);
		try {
			bind.sync().channel().closeFuture().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
