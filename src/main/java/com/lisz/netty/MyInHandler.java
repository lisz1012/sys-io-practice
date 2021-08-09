package com.lisz.netty;


import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.CharsetUtil;

/*
就是用户自己实现的，能让用户放弃在Handler中定义属性吗？
@ChannelHandler.Sharable不能强压给coder
 */
public class MyInHandler extends ChannelInboundHandlerAdapter {
	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		System.out.println("client registered...");
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("client active...");
	}

	// msg是读到的ByteBuf
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf buf = (ByteBuf) msg;
		//CharSequence str = buf.readCharSequence(buf.readableBytes(), CharsetUtil.UTF_8);
		// get不碰index指针，我们要指定起始位置
		CharSequence str = buf.getCharSequence(0, buf.readableBytes(), CharsetUtil.UTF_8);
		System.out.println("Received: " + str);
		ChannelFuture send = ctx.writeAndFlush(buf);
		send.sync();
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		System.out.println("Read complete");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		System.out.println("Exception: " + cause);
	}
}

// 不涉及业务，只注册。为啥要有这么一个init？可以没有，但是MyInHandler就要加
// @ChannelHandler.Sharable. 过桥，完了最好要删除
@ChannelHandler.Sharable
abstract class ChannelInit extends ChannelInboundHandlerAdapter {

	protected abstract void initChannel(ChannelHandlerContext ctx);
//		Channel client = ctx.channel();
//		client.pipeline().addLast(new MyInHandler()); // 2. client::pipeline[ChannelInit, MyInHandler]


	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		initChannel(ctx);
	}
}