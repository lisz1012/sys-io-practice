package com.lisz.netty.rpc.transport;

import com.lisz.netty.rpc.util.PackageMsg;
import com.lisz.netty.rpc.ResponseMappingCallback;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ClientResponse extends ChannelInboundHandlerAdapter {
	// Consumer...
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		PackageMsg packageMsg = (PackageMsg) msg;
		System.out.println(packageMsg.getContent().getRes());

		ResponseMappingCallback.runCallBack(packageMsg);
	}
}
