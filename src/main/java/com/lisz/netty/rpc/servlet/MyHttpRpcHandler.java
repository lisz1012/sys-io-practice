package com.lisz.netty.rpc.servlet;

import com.lisz.netty.rpc.Dispatcher;
import com.lisz.netty.rpc.protocol.MyContent;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MyHttpRpcHandler extends HttpServlet {
		public MyHttpRpcHandler() {
		}

		@Override
		protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			ServletInputStream in = req.getInputStream();
			ObjectInputStream ois = new ObjectInputStream(in);
			MyContent content = null;
			try {
				content = (MyContent) ois.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			String name = content.getName();
			String methodName = content.getMethodName();
			Class<?>[] parameterTypes = content.getParameterTypes();
			Object[] args = content.getArgs();
			Dispatcher dispatcher = Dispatcher.getInstance();
			Object o = dispatcher.get(name);
			Method method = null;
			Object res = null;
			try {
				method = o.getClass().getMethod(methodName, parameterTypes);
				res = method.invoke(o, args);
			} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
				e.printStackTrace();
			}
			MyContent resContent = new MyContent();
			resContent.setRes(res);
			ObjectOutputStream out = new ObjectOutputStream(resp.getOutputStream());
			out.writeObject(resContent);
		}
	}