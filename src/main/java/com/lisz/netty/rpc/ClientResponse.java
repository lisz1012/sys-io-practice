package com.lisz.netty.rpc;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.EventExecutorGroup;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

public class ClientResponse extends ChannelInboundHandlerAdapter {
	// Consumer...
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf buf = (ByteBuf) msg;
		if (buf.readableBytes() >= 100) {
			byte[] bytes = new byte[100];
			buf.readBytes(bytes);
			ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			ObjectInputStream ois = new ObjectInputStream(bais);
			final MyHeader header = (MyHeader) ois.readObject();
			System.out.println("Client response @ ID: " + header.getRequestId());
			// TODO

			ResponseHandler.runCallBack(header.getRequestId());
		}
	}
}
