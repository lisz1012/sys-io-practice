package com.lisz.netty.rpc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResponseMappingCallback {
	public static Map<Long, Runnable> map = new ConcurrentHashMap<>();

	public static void addCallBack(long requestId, Runnable cb) {
		map.put(requestId, cb);
	}

	public static Runnable getCallBack(long requestId) {
		return map.get(requestId);
	}

	public static void runCallBack(long requestId) {
		final Runnable runnable = map.remove(requestId);
		runnable.run();
	}

	public static void countDown(long requestId) {
		final Runnable runnable = map.get(requestId);
		final MyCallBack myCallBack = (MyCallBack) runnable;
		myCallBack.getLatch().countDown();
	}
}
