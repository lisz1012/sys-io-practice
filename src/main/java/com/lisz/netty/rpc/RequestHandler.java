package com.lisz.netty.rpc;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.EventExecutorGroup;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

public class RequestHandler extends ChannelInboundHandlerAdapter {
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		PackageMsg requestPackageMsg = (PackageMsg) msg;
		//System.out.println("Name: " + packageMsg.getContent().getMethodName());
		//System.out.println("Param: " + requestPackageMsg.getContent().getArgs()[0]);

		// 假设业务层已经处理完了，要给客户端返回了
		// 需要注意哪些环节？
		// ByteBuf、因为是RPC，得有一个requestID
		// 在client那边也要解决解码的问题
		// 关注RPC通信协议。来的时候flag是0x14141414，
		// 有新的header + content
		final String ioThreadName = Thread.currentThread().getName();
		// 1. 直接在当前县城处理IO接收处理和返回
		// 2。使用Netty的EventLoop来处理
		// 3. 自己创建线程池
		// 4. 当前线程只处理IO，后续业务以ctx.executor().parent().next().execute的方式让所有线程来分担：IO和业务解耦
		// boss和workers会共享这些线程
		//ctx.executor().execute(new Runnable() { // executor是eventLoop， IO Thread == Exec Thread
		//ctx.executor().parent().submit(new Thread() { // executor().parent()是eventLoopGroup， IO Thread != Exec Thread
		ctx.executor().parent().next().execute(new Thread() { // 上一行和这一行的写法似乎效果一样
			@Override
			public void run() {
				final String execThreadName = Thread.currentThread().getName();
				MyContent content = new MyContent();
				final String s = "IO thread: " + ioThreadName + " Exec thread: " + execThreadName
						+ " From args: " + requestPackageMsg.getContent().getArgs()[0];
				System.out.println(s);
				content.setRes(s);
				final byte[] contentBytes  = SerDerUtil.serialize(content);

				MyHeader myHeader = new MyHeader();
				myHeader.setRequestId(requestPackageMsg.getHeader().getRequestId());
				myHeader.setFlag(0x14141424);
				myHeader.setDataLength(contentBytes.length);
				final byte[] headerBytes = SerDerUtil.serialize(myHeader);

				final ByteBuf byteBuf = PooledByteBufAllocator
						.DEFAULT.directBuffer(headerBytes.length + contentBytes.length);
				byteBuf.writeBytes(headerBytes).writeBytes(contentBytes);
				ctx.writeAndFlush(byteBuf);
			}
		});


//		ByteBuf buf = (ByteBuf) msg;
//		ByteBuf sendBuf = buf.copy();
//		System.out.println("channel start:" + buf.readableBytes());
//		while (buf.readableBytes() >= 100) {
//			byte[] bytes = new byte[100];
//			buf.getBytes(buf.readerIndex(), bytes); // 从哪里读，读多少，但是readindex不变
//			ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
//			ObjectInputStream ois = new ObjectInputStream(bais);
//			final MyHeader header = (MyHeader) ois.readObject();
//			System.out.println("Server received: " + header);
//			if (buf.readableBytes() >= 100 + header.getDataLength()) {
//				buf.readBytes(100); // 只是移动指针到body开始的位置，参数也可以放bytes
//				byte[] data = new byte[(int) header.getDataLength()];
//				buf.readBytes(data);
//				bais = new ByteArrayInputStream(data);
//				ois=  new ObjectInputStream(bais);
//				MyContent content = (MyContent) ois.readObject();
//				System.out.println("Service Name: " + content.getName());
//			} else {
//				System.out.println("Body too short!");
//			}
//		}
//		final ChannelFuture channelFuture = ctx.writeAndFlush(sendBuf);
//		channelFuture.sync();
	}
}
