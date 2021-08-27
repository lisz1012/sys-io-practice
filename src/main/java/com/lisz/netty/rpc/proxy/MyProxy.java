package com.lisz.netty.rpc.proxy;

import com.lisz.netty.rpc.ClientFactory;
import com.lisz.netty.rpc.MyContent;
import com.lisz.netty.rpc.MyHeader;
import com.lisz.netty.rpc.ResponseMappingCallback;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MyProxy {
	public static <T>T proxyGet(Class<T> clazz) {
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
				content.setName(name);
				content.setMethodName(methodName);
				content.setParameterTypes(parameterTypes);
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
				//final CountDownLatch latch = new CountDownLatch(1);
				CompletableFuture<String> res = new CompletableFuture<>();
				ResponseMappingCallback.addCallBack(header.getRequestId(), res);
				final ChannelFuture send = clientChannel.writeAndFlush(buf);
				send.sync();




				//latch.await();

				//5，？，如果从IO ，未来回来了，怎么将代码执行到这里
				//（睡眠/回调，如何让线程停下来？你还能让他继续。。。 Latch）
				return res.get();
			}
		});
	}

	private static MyHeader createHeader(byte[] msgBody) {
		final MyHeader myHeader = new MyHeader();
		int size = msgBody.length; // 　java序列化为啥长度不一样，因为类的全限定名可能跟周老师的有些许不同
		int f = 0x14141414;
		long requestId = Math.abs(UUID.randomUUID().getLeastSignificantBits());
		myHeader.setFlag(f);
		myHeader.setDataLength(size);
		myHeader.setRequestId(requestId);
		return myHeader;
	}
}
