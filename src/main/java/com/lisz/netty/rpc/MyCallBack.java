package com.lisz.netty.rpc;

import lombok.Getter;

import java.util.concurrent.CountDownLatch;

public class MyCallBack implements Runnable {

	@Getter
	private CountDownLatch latch;

	public MyCallBack(CountDownLatch latch) {
		this.latch = latch;
	}

	@Override
	public void run() {
		latch.countDown();
	}
}
