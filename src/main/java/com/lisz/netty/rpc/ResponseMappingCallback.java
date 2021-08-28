package com.lisz.netty.rpc;

import com.lisz.netty.rpc.util.PackageMsg;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ResponseMappingCallback {
	public static Map<Long, CompletableFuture<Object>> map = new ConcurrentHashMap<>();

	public static void addCallBack(long requestId, CompletableFuture<Object> completableFuture) {
		map.put(requestId, completableFuture);
	}

	public static void runCallBack(PackageMsg packageMsg) {
		map.remove(packageMsg.getHeader().getRequestId())
				.complete(packageMsg.getContent().getRes());
	}

//	public static void countDown(long requestId) {
//		final Callable<String> callable = map.get(requestId);
//		final MyCallBack myCallBack = (MyCallBack) callable;
//		myCallBack.getLatch().countDown();
//	}
}
