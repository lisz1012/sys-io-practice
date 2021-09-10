package com.lisz.netty.rpc;
/**
 * @author: 马士兵教育
 * @create: 2020-07-12 20:08
 *
 * 12号的课开始手写RPC ，把前边的IO的课程都看看
 *http://mashibing.com/vip.html#%E5%91%A8%E8%80%81%E5%B8%88%E5%86%85%E5%AD%98%E4%B8%8Eio%E7%A3%81%E7%9B%98io%E7%BD%91%E7%BB%9Cio
 */

/*
    1，先假设一个需求，写一个RPC
    2，来回通信，连接数量，拆包？
    3，动态代理呀，序列化，协议封装
    4，连接池
    5，RPC: 就像调用本地方法一样去调用远程的方法，面向java中就是所谓的 面向interface开发
      表面上是调用接口的方法，其实底层是代理类，实现了远程的调用
 */


import com.lisz.netty.rpc.protocol.MyContent;
import com.lisz.netty.rpc.proxy.MyProxy;
import com.lisz.netty.rpc.service.*;
import com.lisz.netty.rpc.transport.RequestHandler;
import com.lisz.netty.rpc.transport.ServerDecoder;
import com.lisz.netty.rpc.util.SerDerUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.lisz.netty.rpc.proxy.MyProxy.proxyGet;

/**
 * 上节课，基本写了一个能发送
 * 小问题，当并发通过一个连接发送后，服务端解析bytebuf 转 对象的过程出错
 * FC: 函数调用，用户空间
 * SC：System Call： int 0x80
 * RPC：远程过程调用，soocket
 * IPC：管道、信号、socket。同主机内，进程间的通信
 */

public class MyRPCTest {


	// 模拟客户端
	@Test
	public void get() throws Exception {
//		new Thread(()->{startServer();}).start();
		AtomicInteger num = new AtomicInteger(0);
		int size = 500;
		Thread[] threads = new Thread[size];
		for (int i = 0; i < size; i++) {
			threads[i] = new Thread(()->{
//				Fly fly = proxyGet(Fly.class);
//				fly.xxoo("hello");
				Car car = proxyGet(Car.class); //动态代理实现
//				String arg = "hello" + num.incrementAndGet();
//				final String res = car.ooxx(arg);
//				System.out.println("arg: " + arg + " res: " + res);
				System.out.println(car.getPerson("Zhang san", 20));
			});
		}
		for (Thread thread : threads) {
			thread.start();
		}
		System.in.read();


//		Car car = proxyGet(Car.class); //动态代理实现
//		car.ooxx("hello");

	}

	@Test
	public void startHttpServer() {
		/// tomcat jetty
	}

	//Server端
	@Test
	public void startServer() {
		Car car = new MyCar();
		Fly fly = new MyFly();
		Dispatcher dispatcher = Dispatcher.getInstance();
		dispatcher.register(Car.class.getName(), car);
		dispatcher.register(Fly.class.getName(), fly);

		NioEventLoopGroup boss = new NioEventLoopGroup(50);
		NioEventLoopGroup workers = boss;//new NioEventLoopGroup(50);
		ServerBootstrap serverBootstrap = new ServerBootstrap();
		final ChannelFuture bind = serverBootstrap.group(boss, workers)
				.channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<NioSocketChannel>() {
					@Override
					protected void initChannel(NioSocketChannel ch) throws Exception {
						System.out.println("Server accept client port: " + ch.remoteAddress().getPort());
						// 未来有两种可能
						// 1。自定义的RPC
						//ch.pipeline().addLast(new ServerDecoder()).addLast(new RequestHandler(dispatcher));
						// 在自己定义协议的时候，关注过粘包拆包的问题，header + content，header稳定，能给出content的长度
						// 2。小火车，传输协议用的就是http了 <- 可以自己学，通过网络传输的都是字节数组，netty提供一套解码的handler
						ch.pipeline().addLast(new HttpServerCodec())
								.addLast(new HttpObjectAggregator(1024 * 512))
						.addLast(new ChannelInboundHandlerAdapter() {
							/*
							netty 自己提供了一套对于http协议的支持框架
							接收：FullHttpRequest request = (FullHttpRequest) msg
							request -> ByteBuf -> byte[] -> MyContent
							处理，将结果放进一个新的 MyContent并将其写回客户端
							MyContent -> byte[] -> ByteBuf -> response
							发送：ctx.writeAndFlush(response);
							 */
							@Override
							public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
								// http协议，这个msg应该是一个完整的Request，前面的Handler已经帮我们Aggregate好了
								// 可以看HttpObjectAggregator的注释
								//MyContent content = (MyContent) msg;
								FullHttpRequest request = (FullHttpRequest) msg; // 自己想出来的，哈哈哈
								System.out.println(request.toString());//因为现在Consumer使用的是一个现成的URL
								ByteBuf buf = request.content().copy(); // Consumer序列化的MyContent
								byte[] bytes = new byte[buf.readableBytes()];
								buf.readBytes(bytes);
								MyContent content = SerDerUtil.deserialize(bytes, MyContent.class);

								String name = content.getName();
								String methodName = content.getMethodName();
								Class<?>[] parameterTypes = content.getParameterTypes();
								Object[] args = content.getArgs();
								Object o = dispatcher.get(name);
								Method method = o.getClass().getMethod(methodName, parameterTypes);
								final Object retVal = method.invoke(o, args);
								MyContent resContent = new MyContent();
								resContent.setRes(retVal);

								bytes = SerDerUtil.serialize(resContent);
								buf = PooledByteBufAllocator.DEFAULT.directBuffer(bytes.length);
								buf.writeBytes(bytes);
								DefaultFullHttpResponse response = new DefaultFullHttpResponse(
										request.protocolVersion(), HttpResponseStatus.OK, buf);
								// nettyTrans()起作用的时候，必须要设置协议响应中的content-length字段
								response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
								// http协议，header + content。跟上面的FullHttpRequest request = (FullHttpRequest) msg呼应
								ctx.writeAndFlush(response);

								final List<Map.Entry<String, String>> entries = request.headers().entries();
								System.out.println(entries);
							}
						});
					}
				})
				.bind("192.168.1.102", 9090); // 下面还可以继续bing多个端口，但不同端口过来的都会走同一套逻辑👆
		// 当用一个ServerBootStrap，bind一个端口号的时候，boss中有一个EventLoop在listen，accept交给了这个监听线程，
		// 客户端连过来之后，会有一个client socket，它作为结果，会去worker EventLoop的selector上去注册，后者负责数据
		// 的发送和请求的响应。这个过程会把Handler注入到pipeline。
		// 如果bind多个端口，则有多个boss中的EventLoop被激活，listen然后accept。
		// 不同的客户端访问不同的端口号的时候，虽然我说我在了不同的listen的线程上，但是两个端口用的是逻辑是一套Handler逻辑
		// 只不过是丰富地暴露了多个端口号而已。多个端口号映射服务是可以做的，相同的Handler处理
		// 当用两个ServerBootStrap来bind各自的端口号，线程组是一样的，但是每个ServerBootStrap可以配置自己的一套Handlers
		// IO密集型和Kernel打交道；计算密集型消耗CPU时间。
		// 如果CPU被占用的很多，单机解决不了这个问题。跟内核关系不大。上下文切换，开太多线程没有意思
		// IO密集型，好像计算很快，有用户态和内核态切换的问题，因为现在基于内核，我们一般用的都是同步IO模型：程序自己调用read读取到达内核
		// 的数据，而不是内核自动把数据放到用户空间的特定位置。
		// 同一个线程既读取网络IO来的数据，又拿着这个数据进行计算，则就成了Redis的模型；也可以IO和计算有各自的线程，
		// 然后计算线程的CPU只管计算，不怎么被调度了；IO线程也不管计算，这样的IO模型，更顺滑一些
		// 当IO密集型发生的时候，底层会有一个优化：由底层硬件和内核实现的。网卡接收数据会有中断，触发CPU搬运数据。
		// 但是当网卡到来的数据极快，OS操心了，就把中断关了，直接干预数据DMA拷贝。数据包其实先要进入内核的queue，来得太快，
		// 会达到队列大小的上限。程序到底应该以什么频率、速度去搬运内核queue里的数据？阿里、美团都遇到过这个坑。
		// 这里的知识点就在在以上这两个模型里面。起码说在程序员的眼里，能做的就是让线程更多地高频率搬运数据到app内存空间。
		// 这样至少能把用户的请求接进来，不至于拒绝，但是返回的慢一些。如果IO和计算线程混用，可能搬运得不够快，导致丢弃甚至重传，
		// 有点小异常出现，压力就会爆炸！网卡的queue缓存先通过被关闭了中断的CPU拷贝到Kernel内存里，再从后者被拷贝到程序内存里，
		// 计算才能使用它。第二部搬运我们怎么搬？（块设备才有mmap，字符设备是stream）从kernel到程序内存的搬运 （如果JVM程序使用
		// 了多路复用器，则还会有一次拷贝：selector.select()的时候，在JVM里面拿回了可读的文件描述符的集合，放到了JVM的直接空间
		// 然后select.selectedKeys() -> iterator,iterator.remove()， selectedKey返回了一个事件集到JVM对空间，且并不删除之前JVM
		// 里的fd集合，所以要调用 iterator.remove(),这样就会从jvm的直接空间中删除fd，之后在进行select会有一个增量进来，
		// 然后再执行selectedKeys的时候就不会把早先处理过的fd再拷贝到堆里了。通过selectedKeys拿到有数据的fd之后要自己对这些个fd逐一
		// 去调用内核，发起读取各自的数据），一定有一个系统调用的过程，
		// 如何让从kernel内核到JVM直接内存的搬运搬得更快些？这是程序员需要考虑的问题，以避免kernel里的数据积压太多，一般来说，
		// 第一步从网卡到内核的搬运，由CPU直接干预，还是很快的，所谓"网卡打满"的主要原因就是第二部从kernel到JVM直接内存的搬运不够快，
		// 美团和淘宝双十一的树上有这个介绍
		// 一期-203后半节课
		// 并发可以很大：拦截
		// 1。reactor模型，os多路复用器并发模型：epoll
		// 2。EventLoopGroup中每个EventLoop是一个reactor（selector，多路复用器，其上面可以注册很多连接），占用一个CPU
		// 3。IO的读取（从内核到app搬运）是线性的
		// 4。读取到的内容可以在当前线程（读取和计算在一起，阻塞后续IO的读取），也可以在其他线程
		// 5。考量：IO上的损耗，尤其在读取时间和资源占比上
		// 6。尽量发小包（好的压缩：1。协议上减轻，用二进制位 2。好的压缩算法（消耗CPU，但是CPU一定比IO快）。）
		// 无状态：在连接吃梨取连接，并锁定连接，发送和返回的生命周期里锁定
		// 有状态：consumer + Provider端同步实现有状态协议（requestId）。发送和接收可以同步，连接可以共享使用
		// http可以吗？本身是无状态的 C + P。Provider能够解析requestId，处理完了之后还能把它封装回Response中，C端再识别requestId
		// http 的 keepalive可以避免三次握手，一次发送一次返回，再一次发送再一次返回，但是连接不断
		// C + P 端遵从http协议约束，豹纹的封装，是否断开连接，保证发送 + 返回为一个"原子操作"，在http协议之上带上requestId
		// 去掉名字，大家走的都是TCP。内功心法就是TCP连接。但是有可能Provider端不可控，那Consumer可能只能实用http协议
		// http连接可以穿行使用，也有池化的概念
		try {
			bind.sync().channel().closeFuture().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testRPC() {
		Car car = MyProxy.proxyGet(Car.class);
		final Person person = car.getPerson("Zhang san", 12);
		System.out.println(person);
	}
}
