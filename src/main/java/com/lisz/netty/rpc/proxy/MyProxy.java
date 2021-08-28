package com.lisz.netty.rpc.proxy;

import com.lisz.netty.rpc.*;
import com.lisz.netty.rpc.protocol.MyContent;
import com.lisz.netty.rpc.protocol.MyHeader;
import com.lisz.netty.rpc.transport.ClientFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

public class MyProxy {
	public static <T>T proxyGet(Class<T> clazz) {
		//实现各个版本动态代理
		ClassLoader classLoader = clazz.getClassLoader();
		Class<?>[] intfces = {clazz};
		final Dispatcher dispatcher = Dispatcher.getInstance();

		// TODO LOCAL REMOTE 实现：用到Dispatcher直接返回，还是本地调用的时候也代理一下，走代理比较好，可以埋点监控什么的
		return (T) Proxy.newProxyInstance(classLoader, intfces, new InvocationHandler() {
			// TODO 应该在service的方法执行的时候，检查并确定是本地的还是远程的，用到dispatcher来区分一下
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				final Object o = dispatcher.get(clazz.getName());
				if (o == null) {
					// RPC
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

					CompletableFuture<Object> res = ClientFactory.transport(content);
					//latch.await();

					//5，？，如果从IO ，未来回来了，怎么将代码执行到这里
					//（睡眠/回调，如何让线程停下来？你还能让他继续。。。 Latch）
					return res.get();
				} else {
					// Local, 插入一些插件的机会，做一些扩展
					System.out.println("Local FC");
					return method.invoke(o, args);
				}
			}
		});
	}


}
