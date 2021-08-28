package com.lisz.netty.rpc.service;

import com.lisz.netty.rpc.service.Fly;

public class MyFly implements Fly {
	@Override
	public void xxoo(String msg) {
		System.out.println("Server, get client message: " + msg);
	}
}
