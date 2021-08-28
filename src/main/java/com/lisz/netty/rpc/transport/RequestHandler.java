package com.lisz.netty.rpc.transport;

import com.lisz.netty.rpc.Dispatcher;
import com.lisz.netty.rpc.util.PackageMsg;
import com.lisz.netty.rpc.util.SerDerUtil;
import com.lisz.netty.rpc.protocol.MyContent;
import com.lisz.netty.rpc.protocol.MyHeader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RequestHandler extends ChannelInboundHandlerAdapter {
	private Dispatcher dispatcher;
	public RequestHandler(Dispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		PackageMsg requestPackageMsg = (PackageMsg) msg;
		//System.out.println("Name: " + packageMsg.getContent().getMethodName());
		//System.out.println("Param: " + requestPackageMsg.getContent().getArgs()[0]);

		// 假设业务层已经处理完了，要给客户端返回了
		// 需要注意哪些环节？
		// ByteBuf、因为是RPC，得有一个requestID，找回CompletableFuture的内容应该返回给谁
		// 在client那边也要解决解码的问题
		// 关注RPC通信协议。来的时候flag是0x14141414，
		// 有新的header + content
		final String ioThreadName = Thread.currentThread().getName();
		// 1. 直接在当前线程处理IO接收处理和返回
		// 2。使用Netty的EventLoop来处理
		// 3. 自己创建线程池. 好处是自定义灵活
		// 4. 当前线程只处理IO，后续业务以ctx.executor().parent().next().execute的方式让所有线程来分担：IO和业务解耦
		// boss和workers会共享这些线程
		// ctx.executor().execute(new Runnable() { // executor是eventLoop， IO Thread == Exec Thread，上面第一种.
		//ctx.executor().parent().submit(new Thread() { // executor().parent()是eventLoopGroup， IO Thread != Exec Thread
		ctx.executor().parent().next().execute(new Thread() { // 上一行和这一行的写法似乎效果一样
			@Override
			public void run() {
				// 反射。Spark里面具备固定角色，单例对象msg，判定。还有一种方法：javaassist动态代理的方式生成调用类。反射最慢
				// 剩下两种看情况，看用户的实现和接口确不确定（一撅腚就知道放什么屁 VS 一撅腚不知道放什么屁）
				final String className = requestPackageMsg.getContent().getName();
				final String methodName = requestPackageMsg.getContent().getMethodName();
				final Class<?>[] parameterTypes = requestPackageMsg.getContent().getParameterTypes();
				final Object[] args = requestPackageMsg.getContent().getArgs();
				final Object o = dispatcher.get(className);
				Object retVal = null;
				try {
					final Method method = o.getClass().getMethod(methodName, parameterTypes);
					retVal = method.invoke(o, args);
				} catch (InvocationTargetException | IllegalAccessException| NoSuchMethodException e) {
					e.printStackTrace();
				}


//				final String execThreadName = Thread.currentThread().getName();
				MyContent content = new MyContent();
//				final String s = "IO thread: " + ioThreadName + " Exec thread: " + execThreadName
//						+ " From args: " + requestPackageMsg.getContent().getArgs()[0];
				content.setRes((String) retVal);
				final byte[] contentBytes  = SerDerUtil.serialize(content);

				MyHeader myHeader = new MyHeader();
				myHeader.setRequestId(requestPackageMsg.getHeader().getRequestId());
				myHeader.setFlag(0x14141424);
				myHeader.setDataLength(contentBytes.length);
				final byte[] headerBytes = SerDerUtil.serialize(myHeader);
				//下面两行相当于ByteBuf buf = Unpooled.copiedBuffer(msgHeader, msgBody);
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
