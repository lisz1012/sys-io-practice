package com.lisz.netty.rpc.protocol;

import lombok.Data;

import java.io.Serializable;

@Data
public class MyContent implements Serializable {
	String name;
	String methodName;
	Class<?>[] parameterTypes;
	Object[] args;
	Object res;
}
