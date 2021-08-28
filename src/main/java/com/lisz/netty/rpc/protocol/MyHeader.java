package com.lisz.netty.rpc.protocol;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.UUID;

// header 是定长的
@ToString
@Data
public class MyHeader implements Serializable {
	// 通信上的协议
	/*
		标识协议的
		UUID
		Data length
	 */
	private int flag;
	private long requestId;
	private long dataLength;

	public static MyHeader createHeader(byte[] msgBody) {
		final MyHeader myHeader = new MyHeader();
		int size = msgBody.length; // 　java序列化为啥长度不一样，因为类的全限定名可能跟周老师的有些许不同
		int f = 0x14141414;
		long requestId = Math.abs(UUID.randomUUID().getLeastSignificantBits());
		myHeader.setFlag(f);
		myHeader.setDataLength(size);
		myHeader.setRequestId(requestId);
		return myHeader;
	}
}
