package test.moniMaster;

import org.apache.curator.RetryPolicy;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import gate.util.MixAll;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * 模拟 模拟前置
 * 前置（规约解析服务）是翻译和编码报文的实际处理者
 * @author BriansPC
 *
 */
public class moniMaster {
	public static void main(String[] args) {
		
		String zkAddr = "127.0.0.1:9689,127.0.0.1:9690,127.0.0.1:9691";
		
		EventLoopGroup boss=new NioEventLoopGroup();
		EventLoopGroup work=new NioEventLoopGroup();
		//创建ServerBootstrap辅助类  客户端是Bootstrap辅助类 注意区分
		ServerBootstrap bootstrap=new ServerBootstrap();
		//通过辅助类配置通道参数
		bootstrap.group(boss,work);
		bootstrap.channel(NioServerSocketChannel.class);
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
		//关联通道的处理类
		bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

			@Override
			protected void initChannel(SocketChannel sc) throws Exception {
				
				sc.pipeline().addLast(new moniMasterDecoder());
				sc.pipeline().addLast(new moniMasterHandler());
			}
		});
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				ChannelFuture channelFuture;
				try {
					channelFuture = bootstrap.bind(8888).sync();
					System.out.println("模拟前置已启动！！port = " +8888);
					
					channelFuture.channel().closeFuture().sync();
					
					boss.shutdownGracefully();
					work.shutdownGracefully();
				} catch (InterruptedException e) {
					
					e.printStackTrace();
				}
			}
		},"moniQZThread").start();
		
		
		
		//如果使用单机版请注掉下面的代码，则模拟前置不会继续连接zookeeper
		
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 10);
		CuratorFramework cf = CuratorFrameworkFactory.builder()
					.connectString(zkAddr)
					.sessionTimeoutMs(6000)
					.retryPolicy(retryPolicy)
					.build();
		System.out.println("zk连接中。。。。。。");
		//3 开启连接
		cf.start();
		while(cf.getState() != CuratorFrameworkState.STARTED){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("zk连接成功。。。。。");


		try {
			String addr = MixAll.linuxLocalIP();
			cf.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath("/iotGate2Master/"+addr,addr.getBytes());
			System.out.println("********zookeeper注册前置信息成功！********");
		} catch (Exception e) {
			System.err.println("zookeeper注册前置信息失败");
		}
		
	}
}
