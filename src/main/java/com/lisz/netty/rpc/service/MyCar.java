package com.lisz.netty.rpc.service;

public class MyCar implements Car {
	@Override
	public Person getPerson(String name, Integer age) {
		Person person = new Person(name, age);
		return person;
	}

	@Override
	public String ooxx(String msg) {
		System.out.println("Server, get client message: " + msg);
		return "Server response " + msg;
	}
}
