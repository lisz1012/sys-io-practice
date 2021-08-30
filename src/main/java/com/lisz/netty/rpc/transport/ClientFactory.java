package com.lisz.netty.rpc.transport;

import com.lisz.netty.rpc.ResponseMappingCallback;
import com.lisz.netty.rpc.util.SerDerUtil;
import com.lisz.netty.rpc.protocol.MyContent;
import com.lisz.netty.rpc.protocol.MyHeader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

// 源于Spark源码 Spark RpcEnv
public class ClientFactory {
	private int poolSize = 5;
	private static final ClientFactory INSTANCE = new ClientFactory();
	private ClientFactory(){}

	public static ClientFactory getInstance() {
		return INSTANCE;
	}

	// 一个Consumer可以连接多个Provider，每一个Provider 都有自己的pool，kv
	private Map<InetSocketAddress, ClientPool> outboxes = new ConcurrentHashMap<>();

	public NioSocketChannel getClient(InetSocketAddress address) {
		// TODO 在并发情况下一定要谨慎
		ClientPool clientPool = outboxes.get(address);
		if (clientPool == null) {
			synchronized (outboxes) {
				if (clientPool == null) {
					outboxes.putIfAbsent(address, new ClientPool(poolSize, address));
					clientPool = outboxes.get(address);
				}
			}
		}
		return clientPool.getNioSocketChannel();
	}

	public static CompletableFuture<Object> transport(MyContent content) throws Throwable {
		// content就是货物，现在可以用自定义的RPC传输协议，也可以用Http协议作为载体传输
		// 先手工用了http协议作为载体，那样是不是未来可以让Provider是一个tomcat jetty，基于http协议的一个容器
		// 有无状态来自于你使用什么协议，http协议是无状态的（自定义的是有状态的），每请求对应一个连接。
		// Dubbo是一个RPC框架，netty是一个io框架。Dubbo中传输协议上，可以是自定义的RPC传输协议，或者http协议
		// TODO 未来协议可能会变

		byte[] msgBody = SerDerUtil.serialize(content);

		//2，requestID+message  ，本地要缓存
		//协议：【header<>】【msgBody
		MyHeader header = MyHeader.createHeader(msgBody);

		// TODO: Server: dispatcher Excecutor
		byte[] msgHeader = SerDerUtil.serialize(header);

		ByteBuf buf = Unpooled.copiedBuffer(msgHeader, msgBody);
		/**
		 * 1。缺失了注册发现，zk
		 * 2。第一层负载，面向Provider
		 * 3。Consumer是线程池，面向Service，开启若干条连接的意义是适应不同的远程方法调用
		 *    并发就有木桶效应，倾斜。一个Service可能有多个物理主机，一个物理主机可与开启
		 *    多个连接
		 *    ipA：port
		 *          socket1
		 *          socket2
		 *    ipB：port
		 */
		//3，连接池：：取得连接
		ClientFactory factory = ClientFactory.getInstance();
		NioSocketChannel clientChannel
				= factory.getClient(new InetSocketAddress("192.168.1.102", 9090));
		//4，发送--> 走IO  out -->走Netty（event 驱动）
		//final CountDownLatch latch = new CountDownLatch(1);
		CompletableFuture<Object> res = new CompletableFuture<>();
		ResponseMappingCallback.addCallBack(header.getRequestId(), res);
		final ChannelFuture send = clientChannel.writeAndFlush(buf);
		send.sync();

		return res;
	}
}
