package com.zookeeper.queue;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;

/**
 * Created by wangruli on 2017/10/11.
 */
public class QueueZooKeeper {


	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			doOne();
		} else {
			doAction(Integer.parseInt(args[0]));
		}
	}

	public static ZooKeeper getConnection(String host) throws IOException {
		ZooKeeper zooKeeper = new ZooKeeper(host, 60000, new Watcher() {
			// 监控所有被触发的事件
			public void process(WatchedEvent event) {
				if (event.getType() == Event.EventType.NodeCreated && event.getPath().equals("/queue/start")) {
					System.out.println("队列已经完成！");
				}
			}
		});
	   return zooKeeper;
	}

	//单节点测试
	public static void doOne() throws  Exception{
         ZooKeeper zooKeeper = getConnection("192.168.0.33:2181");
         initQueue(zooKeeper);
         for (int i = 1; i<11;i++){
         	joinQueue(zooKeeper,i);
		 }
	}

	//分布式模拟实验
	public static void doAction(int client) throws Exception{
		String host1 = "192.168.1.201:2181";
		String host2 = "192.168.1.201:2182";
		String host3 = "192.168.1.201:2183";

		ZooKeeper zooKeeper = null;
		switch (client) {
			case 1:
				zooKeeper = getConnection(host1);
				initQueue(zooKeeper);
				joinQueue(zooKeeper, 1);
				break;
			case 2:
				zooKeeper = getConnection(host2);
				initQueue(zooKeeper);
				joinQueue(zooKeeper, 2);
				break;
			case 3:
				zooKeeper = getConnection(host3);
				initQueue(zooKeeper);
				joinQueue(zooKeeper, 3);
				break;
		}
	}

	/**
	 * 初始化队列信息
	 * @param zooKeeper
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public static void initQueue(ZooKeeper zooKeeper) throws KeeperException, InterruptedException{
		System.out.println("设置监控/queue/start");
        zooKeeper.exists("/queue/start",true);
		Stat stat = zooKeeper.exists("/queue",false);
		if(stat == null){
			System.out.println("创建队列:::::/queue task-queue");
           zooKeeper.create("/queue",new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT);
		}else{
			System.out.println("创建队列queue已经存在");
		}
	}

	/**
	 * 成员入队操作
	 * @param zooKeeper
	 * @param x
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public static void joinQueue(ZooKeeper zooKeeper,int x) throws KeeperException, InterruptedException{
		System.out.println("队列成员"+x+"进入队列");
		zooKeeper.create("/queue/member_"+x,new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.EPHEMERAL_SEQUENTIAL);
		isCompleted(zooKeeper);
	}

	/**
	 * 判断是否入队完成
	 * @param zooKeeper
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public static void isCompleted(ZooKeeper zooKeeper) throws KeeperException, InterruptedException{
		int size = 10;
		int length = zooKeeper.getChildren("/queue",true).size();
		System.out.println("入队数量:" + length + "/" + size);
		if (length >= size) {
			zooKeeper.create("/queue/start", "start".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
			System.out.println("创建节点 /queue/start 完成");
		}
	}

}
