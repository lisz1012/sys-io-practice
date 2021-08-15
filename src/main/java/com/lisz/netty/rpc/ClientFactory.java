package com.lisz.netty.rpc;

import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 源于Spark源码 Spark RpcEnv
public class ClientFactory {
	private int poolSize = 1;
	private static final ClientFactory INSTANCE = new ClientFactory();
	private ClientFactory(){}

	public static ClientFactory getInstance() {
		return INSTANCE;
	}

	// 一个Consumer可以连接多个Provider，每一个Provider 都有自己的pool，kv
	private Map<InetSocketAddress, ClientPool> outboxes = new ConcurrentHashMap<>();

	public NioSocketChannel getClient(InetSocketAddress address) {
		ClientPool clientPool = outboxes.get(address);
		if (clientPool == null) {
			outboxes.put(address, new ClientPool(poolSize, address));
			clientPool = outboxes.get(address);
		}
		return clientPool.getNioSocketChannel();
	}
}
