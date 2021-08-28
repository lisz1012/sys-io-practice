package com.lisz.netty.rpc.util;

import com.lisz.netty.rpc.protocol.MyContent;
import com.lisz.netty.rpc.protocol.MyHeader;
import lombok.Data;

@Data
public class PackageMsg {
	private MyHeader header;
	private MyContent content;

	public PackageMsg(MyHeader header, MyContent content) {
		this.header = header;
		this.content = content;
	}
}
