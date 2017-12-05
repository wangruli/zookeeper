package com.zookeeper.synchronizedLock;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Created by wangruli on 2017/10/10.
 */
public class ZookeeperLock {

	private static String ROOT_LOCK_PATH = "/Locks";
	private String PRE_LOCK_NAME = "mylock_";
	private static String zookeeperIp = "192.168.0.33:2181";
	private static ZooKeeper zkClient  = null;

	private static ZookeeperLock lock;
	public static ZookeeperLock getInstance(){
		if(null == lock){
			lock = new ZookeeperLock();
		}
		if(null == zkClient) {
			try {
				zkClient = new ZooKeeper(zookeeperIp, 3000, null);
				Stat stat = zkClient.exists(ROOT_LOCK_PATH, false);
				if(stat == null){
					// 创建根节点
					zkClient.create(ROOT_LOCK_PATH, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT);
				}
			}catch (IOException e) {
				e.printStackTrace();
			} catch (KeeperException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return lock;
	}

	/**
	 * 获取锁：实际上是创建线程目录，并判断线程目录序号是否最小
	 * @return
	 */
	public String getLock(){
		try{
			// 关键方法，创建包含自增长id名称的目录，这个方法支持了分布式锁的实现
			// 四个参数：
			// 1、目录名称 2、目录文本信息
			// 3、文件夹权限，Ids.OPEN_ACL_UNSAFE表示所有权限
			// 4、目录类型，CreateMode.EPHEMERAL_SEQUENTIAL表示会在目录名称后面加一个自增加数字
			String lockPath = zkClient.create(ROOT_LOCK_PATH+'/' + PRE_LOCK_NAME, Thread.currentThread().getName().getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE,
					CreateMode.EPHEMERAL_SEQUENTIAL);
			System.out.println(Thread.currentThread().getName() + " create lock path : " + lockPath);
			tryLock(lockPath);
			return lockPath;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private boolean tryLock(String lockPath) throws KeeperException, InterruptedException {
		// 获取ROOT_LOCK_PATH下所有的子节点，并按照节点序号排序
		List<String> lockPaths = zkClient.getChildren(ROOT_LOCK_PATH, false);
		Collections.sort(lockPaths);
		int index = lockPaths.indexOf(lockPath.substring(ROOT_LOCK_PATH.length() + 1));
		if (index == 0) {
			return true;
		}
		else {
			// 创建Watcher，监控lockPath的前一个节点
			Watcher watcher = new Watcher() {
				public void process(WatchedEvent event) {
					// 创建的锁目录只有删除事件
					System.out.println("Received delete event, node path is " + event.getPath());
					synchronized (this) {
						notifyAll();
					}
				}
			};
			String preLockPath = lockPaths.get(index - 1);
			// 查询前一个目录是否存在，并且注册目录事件监听器，监听一次事件后即删除
			Stat state =zkClient.exists(ROOT_LOCK_PATH + "/" + preLockPath, watcher);
			// 返回值为目录详细信息
			if (state == null) {
				return tryLock(lockPath);
			}
			else {
				synchronized (watcher) {
					// 等待目录删除事件唤醒
					watcher.wait();
				}
				return tryLock(lockPath);
			}
		}
	}

	/**
	 * 释放锁：实际上是删除当前线程目录
	 * @param lockPath
	 */
	public void releaseLock(String lockPath) {
		try {
			zkClient.delete(lockPath, -1);
			System.out.println("Release lock, lock path is" + lockPath);
		}
		catch (InterruptedException  e) {
			e.printStackTrace();
		}
		catch ( KeeperException e){
			e.printStackTrace();
		}
	}
}
