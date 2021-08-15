package com.lisz.netty.rpc;

import io.netty.buffer.ByteBuf;
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
		ByteBuf buf = (ByteBuf) msg;
		ByteBuf sendBuf = buf.copy();
		if (buf.readableBytes() >= 100) {
			byte[] bytes = new byte[100];
			buf.readBytes(bytes);
			ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			ObjectInputStream ois = new ObjectInputStream(bais);
			final MyHeader header = (MyHeader) ois.readObject();
			System.out.println("Server received: " + header);
			if (buf.readableBytes() >= header.getDataLength()) {
				byte[] data = new byte[(int) header.getDataLength()];
				buf.readBytes(data);
				bais = new ByteArrayInputStream(data);
				ois=  new ObjectInputStream(bais);
				MyContent content = (MyContent) ois.readObject();
				System.out.println("Service Name: " + content.getName());
			}
		}
		final ChannelFuture channelFuture = ctx.writeAndFlush(sendBuf);
		channelFuture.sync();
	}
}
