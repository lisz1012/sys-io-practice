package com.lisz.netty.rpc;

public class MyCar implements Car {
	@Override
	public String ooxx(String msg) {
		System.out.println("Server, get client message: " + msg);
		return "Server response " + msg;
	}
}
