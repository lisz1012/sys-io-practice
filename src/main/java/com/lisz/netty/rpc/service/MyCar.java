package com.lisz.netty.rpc.service;

import com.lisz.netty.rpc.service.Car;

public class MyCar implements Car {
	@Override
	public String ooxx(String msg) {
		System.out.println("Server, get client message: " + msg);
		return "Server response " + msg;
	}
}
