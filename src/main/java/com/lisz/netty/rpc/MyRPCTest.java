package com.lisz.netty.rpc;
/**
 * @author: 马士兵教育
 * @create: 2020-07-12 20:08
 *
 * 12号的课开始手写RPC ，把前边的IO的课程都看看
 *http://mashibing.com/vip.html#%E5%91%A8%E8%80%81%E5%B8%88%E5%86%85%E5%AD%98%E4%B8%8Eio%E7%A3%81%E7%9B%98io%E7%BD%91%E7%BB%9Cio
 */

/*
    1，先假设一个需求，写一个RPC
    2，来回通信，连接数量，拆包？
    3，动态代理呀，序列化，协议封装
    4，连接池
    5，RPC: 就像调用本地方法一样去调用远程的方法，面向java中就是所谓的 面向interface开发
      表面上是调用接口的方法，其实底层是代理类，实现了远程的调用
 */


import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Comparator;

/**
 * 上节课，基本写了一个能发送
 * 小问题，当并发通过一个连接发送后，服务端解析bytebuf 转 对象的过程出错
 */

public class MyRPCTest {
	// 模拟客户端
	@Test
	public void get() {
		Car car = proxyGet(Car.class); //动态代理实现
		car.ooxx("hello");
		Fly fly = proxyGet(Fly.class);
		fly.xxoo("hello");
	}

	private static <T>T proxyGet(Class<T> clazz) {
		//实现各个版本动态代理
		ClassLoader classLoader = clazz.getClassLoader();
		Class<?>[] intfces = {clazz};
		return (T) Proxy.newProxyInstance(classLoader, intfces, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				//如何设计我们的consumer对于provider的调用过程
				//1，调用 服务，方法，参数  ==》 封装成message  [content]

				//2，requestID+message  ，本地要缓存
				//协议：【header<>】【msgBody】

				//3，连接池：：取得连接

				//4，发送--> 走IO  out -->走Netty（event 驱动）

				//5，？，如果从IO ，未来回来了，怎么将代码执行到这里
				//（睡眠/回调，如何让线程停下来？你还能让他继续。。。）

				return null;
			}
		});
	}
}
