package com.lisz.netty.rpc;

public class MyFly implements Fly {
	@Override
	public void xxoo(String msg) {
		System.out.println("Server, get client message: " + msg);
	}
}
