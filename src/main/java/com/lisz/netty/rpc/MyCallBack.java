package com.lisz.netty.rpc;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

public class MyCallBack<String> implements Callable<String> {
	@Setter
	private String res;

	@Override
	public String call() {
		return res;
	}
}
