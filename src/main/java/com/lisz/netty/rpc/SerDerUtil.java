package com.lisz.netty.rpc;

import java.io.*;

public class SerDerUtil {
	public synchronized static byte[] serialize(Object o) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
			 ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(o);
			final byte[] bytes = baos.toByteArray();
			return bytes;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public synchronized static <T>T deserialize(byte[] bytes, Class<T> clazz) {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		     ObjectInputStream ois = new ObjectInputStream(bais)) {
			return (T) ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
}
