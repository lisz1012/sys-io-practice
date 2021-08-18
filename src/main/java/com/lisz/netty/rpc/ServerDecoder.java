package com.lisz.netty.rpc;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.List;
/**
 * 解码器类
 */
public class ServerDecoder extends ByteToMessageDecoder {
	/*
	父类里一定有channelRead {前面的品buf  decode)_; 剩余留存; 堆out遍历 } -> byteBuf
	我们用Netty就是偷懒了，自己其实也能实现
	只处理囫囵的 	PackageMsg，遇到半截的就break返回，父类里有帮着处理的代码
	 */
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
		//System.out.println("channel start:" + buf.readableBytes());
		while (buf.readableBytes() >= 100) {
			byte[] bytes = new byte[100];
			buf.getBytes(buf.readerIndex(), bytes); // 从哪里读，读多少，但是readindex不变
			ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			ObjectInputStream ois = new ObjectInputStream(bais);
			final MyHeader header = (MyHeader) ois.readObject();
			//System.out.println("Server received: " + header);
			if (buf.readableBytes() >= 100 + header.getDataLength()) {
				buf.readBytes(100); // 只是移动readIndex指针到body开始的位置，参数也可以放bytes
				byte[] data = new byte[(int) header.getDataLength()];
				buf.readBytes(data);
				bais = new ByteArrayInputStream(data);
				ois=  new ObjectInputStream(bais);

				if (header.getFlag() == 0x14141414) { //  Client -> Server
					MyContent content = (MyContent) ois.readObject();
					//System.out.println("Service Name: " + content.getName());
					out.add(new PackageMsg(header, content));
				} else if (header.getFlag() == 0x14141424) {
					MyContent content = (MyContent) ois.readObject();
					//System.out.println("Service Name: " + content.getName());
					out.add(new PackageMsg(header, content));
				}
			} else {
				break;
			}
		}
	}
}
