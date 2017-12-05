package com.zookeeper.synchronizedLock;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created by wangruli on 2017/10/10.
 */
public class LockTest implements Watcher {

	private String zookeeperIp = "192.168.0.33:2181";

	@Test
	public void testConnection() throws IOException{
		try{
			// 创建一个与服务器的连接
			ZooKeeper zk = new ZooKeeper(zookeeperIp, 3000, new Watcher() {
				// 监控所有被触发的事件
				public void process(WatchedEvent event) {
					System.out.println("已经触发了" + event.getType() + "事件！");
				}
			});
			// 创建一个目录节点
			zk.create("/testRootPath", "testRootData".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			// 创建一个子目录节点
			zk.create("/testRootPath/testChildPathOne", "testChildDataOne".getBytes(),
					ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT);
			System.out.println(new String(zk.getData("/testRootPath",false,null)));
			// 取出子目录节点列表
			System.out.println(zk.getChildren("/testRootPath",true));
			// 修改子目录节点数据
			zk.setData("/testRootPath/testChildPathOne","modifyChildDataOne".getBytes(),-1);
			System.out.println("目录节点状态：["+zk.exists("/testRootPath",true)+"]");
			// 创建另外一个子目录节点
			zk.create("/testRootPath/testChildPathTwo", "testChildDataTwo".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT);
			System.out.println(new String(zk.getData("/testRootPath/testChildPathTwo",true,null)));
			// 删除子目录节点
			zk.delete("/testRootPath/testChildPathTwo",-1);
			zk.delete("/testRootPath/testChildPathOne",-1);
			// 删除父目录节点
			zk.delete("/testRootPath",-1);
			// 关闭连接
			zk.close();
		}catch (InterruptedException  e) {
			e.printStackTrace();
		}
		catch ( KeeperException e){
			e.printStackTrace();
		}
	}


	private ZooKeeper zk;

	// Zookeeper 锁根
	private static final String LOCK_ROOT = "/Lock";
	// 当前客户端注册锁名
	private String lockName;
	// 线程锁，用于没有获得执行锁时阻塞
	private Integer threadLock = -1;

	public LockTest(String server) throws Exception {
		zk = new ZooKeeper(server, 6000, this);
	}

	/**
	 * 注册锁
	 *
	 * @throws Exception
	 */
	public void lock() throws Exception {
		// 注册锁根结点
		Stat rootStat = zk.exists(LOCK_ROOT, false);
		if (rootStat == null) {
			String rootPath = zk.create(LOCK_ROOT, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			System.out.println("Create root : " + rootPath);
		}
		// 在服务器注册自己的锁
		lockName = zk.create(LOCK_ROOT + "/lock_", new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
		System.out.println("Create lock : " + lockName);
		// 循环判断服务器当前锁是否为自己的锁
		while (true) {
			// 设置观察
			List<String> lockList = zk.getChildren(LOCK_ROOT, true);
			Collections.sort(lockList);
			if (!lockName.endsWith(lockList.get(0))) {
				// 当前锁非本服务器注册，等待
				synchronized (threadLock) {
					threadLock.wait();
				}
			} else {
				// 获得锁成功
				return;
			}
		}
	}

	/**
	 * 释放锁
	 *
	 * @throws Exception
	 */
	public void unLock() throws Exception {
		Stat stat = zk.exists(lockName, false);
		if (stat != null) {
			zk.delete(lockName, stat.getVersion());
		}
	}

	/**
	 * 竞争锁
	 */
	public void process(WatchedEvent event) {
		// 事件发生在锁目录上
		String path = event.getPath();
		if (path != null && path.startsWith(LOCK_ROOT)) {
			// 监控的是root node 需要判断node children changed 事件
			if (event.getType() == Event.EventType.NodeChildrenChanged) {
				// 事件类型为锁删除事件,激活锁判断
				synchronized (threadLock) {
					threadLock.notify();
				}
			}
		}
	}

	public void shutdown() throws InterruptedException {
		zk.close();
	}

	/**
	 * 期望结果：线程处理过程没有多个线程的嵌套流程
	 */
	public static void main(String[] args) throws Exception {
		for (int i = 0; i < 5; i++) {
			new Thread(new Runnable() {
				public void run() {
					try {
						String server = "192.168.0.33:2181";
						LockTest lockTest = new LockTest(server);
						// 注册锁
						lockTest.lock();
						// 执行获取锁后的任务
						System.out.println("Thread :　" + Thread.currentThread().getId() + " start ! ");
						Thread.sleep(1000 + new Random().nextInt(5000));
						System.out.println("Thread :　" + Thread.currentThread().getId() + " finish ! ");
						// 获取锁后的任务执行完毕，释放
						lockTest.unLock();
						lockTest.shutdown();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}
}
