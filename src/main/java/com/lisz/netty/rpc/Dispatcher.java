package com.lisz.netty.rpc;

import java.util.concurrent.ConcurrentHashMap;

public class Dispatcher {
	public static ConcurrentHashMap<String, Object> invokeMap = new ConcurrentHashMap<>();

	public void register(String className, Object obj) {
		invokeMap.put(className, obj);
	}

	public Object get(String className) {
		return invokeMap.get(className);
	}
}
