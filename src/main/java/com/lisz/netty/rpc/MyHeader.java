package com.lisz.netty.rpc;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

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
}
