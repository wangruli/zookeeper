package com.zookeeper.synchronizedLock;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by wangruli on 2017/10/10.
 */
public class ZKConnector implements Watcher {

	private static final Logger logger = LoggerFactory.getLogger(ZKConnector.class);

	private CountDownLatch connectedSemaphore = new CountDownLatch(1);
	private ZooKeeper zk =null;

	private static ZKConnector lock;
	public static ZKConnector getInstance(){
		if(null == lock){
			lock = new ZKConnector();
		}
		return lock;
	}

	/**
	 * 释放zookeeper连接
	 */
	public void releaseConnection() {
		if (this.zk!=null) {
			try {
				this.zk.close();
			} catch ( InterruptedException e ) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 创建zookeeper的连接
	 * @param connectString zookeeper服务器地址列表
	 * @param sessionTimeout Session超时时间
	 */
	public void createConnection(String connectString, int sessionTimeout) {
		//先释放zookeeper连接
		this.releaseConnection();
		try {
			zk = new ZooKeeper( connectString, sessionTimeout, this);
			connectedSemaphore.await();
		} catch ( InterruptedException e ) {
			logger.info( "连接创建失败，发生 InterruptedException");
			e.printStackTrace();
		} catch (IOException e ) {
			logger.info( "连接创建失败，发生 IOException" );
			e.printStackTrace();
		}
	}

	/**
	 * 检查Znode是否为空
	 */
	public boolean check(String zNode){
		try {
			return this.zk.exists(zNode, false).getDataLength()>0;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}catch (KeeperException e){
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 检查zNode是否存在
	 * 不为空 返回true
	 * 为空，则返回false
	 */
	public boolean exist(String zNode){
		try {
			Stat stat =this.zk.exists(zNode, false);
			return stat==null?false:true;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}catch (KeeperException e){
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * 读取zNode的数据
	 */
	public String readData(String path){
		try {
			if(this.zk.exists(path, false) == null){
				return "";
			}
			return new String( this.zk.getData(path, false, null));
		} catch ( KeeperException e){
			logger.info("读取数据失败，发生KeeperException，path: " + path);
			e.printStackTrace();
			return "";
		} catch ( InterruptedException e){
			logger.info("读取数据失败，发生 InterruptedException，path: " + path);
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * 更新zNode的数据
	 */
	public boolean writeData(String path,String data){
		try {
			if(this.zk.exists(path, false) == null){
				return createPersistNode(path,data);
			}else{
				deleteNode(path);
				createPersistNode(path,data);
			}
		} catch ( KeeperException e ) {
			logger.info( "更新数据失败，发生KeeperException，path: " + path  );
			e.printStackTrace();
		} catch ( InterruptedException e ) {
			logger.info( "更新数据失败，发生 InterruptedException，path: " + path  );
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 获取子节点数据
	 */
	public List<String> getChildren(String node){
		try {
			List<String> subNodes = this.zk.getChildren(node, false);
			return subNodes;
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (KeeperException e){
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 创建持久化节点
	 * @param path 节点path
	 * @param data    初始数据内容
	 * @return
	 */
	public boolean createPersistNode(String path, String data) {
		try {
			String createpath =this.zk.create(path,data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT_SEQUENTIAL);
			logger.info("节点创建成功, Path: " + createpath + ", content: " + data );
			return true;
		} catch ( KeeperException e ) {
			logger.info( "节点创建失败，发生KeeperException" );
			e.printStackTrace();
		} catch ( InterruptedException e ) {
			logger.info( "节点创建失败，发生 InterruptedException" );
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 创建短暂序列化节点
	 * @param path 节点path
	 * @param data 初始数据内容
	 * @return
	 */
	public  String createEsquentialNode(String path, String data) {
		String sequentialNode = null;
		try {
			sequentialNode =this.zk.create(path,data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.EPHEMERAL_SEQUENTIAL);
			logger.info("节点创建成功, Path: " + sequentialNode + ", content: " + data );
			return sequentialNode;
		} catch ( KeeperException e ) {
			logger.info( "节点创建失败，发生KeeperException" );
			e.printStackTrace();
		} catch ( InterruptedException e ) {
			logger.info( "节点创建失败，发生 InterruptedException" );
			e.printStackTrace();
		}
		return null;
	}


	/**
	 * 删除节点
	 */
	public void deleteNode(String path) {
		try {
			if(this.zk.exists(path, false) == null){
				logger.info("该节点不存在！不做任何操作" );
			}else{
				this.zk.delete(path, -1);
				logger.info("删除节点成功，path："+ path);
			}
		} catch ( KeeperException e ) {
			logger.info("删除节点失败，发生KeeperException，path: " + path);
			e.printStackTrace();
		} catch ( InterruptedException e ) {
			logger.info("删除节点失败，发生 InterruptedException，path: " + path);
			e.printStackTrace();
		}
	}

	public void process(WatchedEvent event) {
		logger.info( "收到事件通知：" + event.getState() +"\n"  );
		// 连接建立, 回调process接口时, 其event.getState()为KeeperState.SyncConnected
		if (Event.KeeperState.SyncConnected == event.getState() ) {
			// 放开闸门, wait在connect方法上的线程将被唤醒
			connectedSemaphore.countDown();
		}
	}

	// Zookeeper 锁根
	private static final String LOCK_ROOT = "/Lock";
	// 当前客户端注册锁名
	private String lockName;
	// 线程锁，用于没有获得执行锁时阻塞
	private Integer threadLock = -1;

	/**
	 * 获取共享锁
	 * @return
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public void getLock() throws KeeperException, InterruptedException {
		//注册锁根节点
		Stat rootStat = this.zk.exists(LOCK_ROOT,false);
		if(rootStat == null){
			String rootPath = this.zk.create(LOCK_ROOT,new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT);
			logger.info("创建锁根节点成功，rootPath: "+rootPath);
		}
		// 在服务器注册自己的锁(临时的排序节点)
		lockName = zk.create(LOCK_ROOT+"/lock_",new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.EPHEMERAL_SEQUENTIAL);
		logger.info("创建排序节点成功，lock: "+lockName);
		//判断服务器当前的锁是否为自己创建的锁
		while (true){
			List<String> lockList = zk.getChildren(LOCK_ROOT,true);
			Collections.sort(lockList);
			if(!lockName.endsWith(lockList.get(0))){
				//如果不是自己注册的锁，等待
				synchronized (this.threadLock){
					threadLock.wait();
				}
			}else{
				//获取锁成功
				return;
			}
		}
	}

	/**
	 * 递归算法获取共享锁
	 * @return
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	private boolean tryLock() throws KeeperException, InterruptedException {
		//在zookeeper上注册根节点
		Stat rootStat = this.zk.exists(LOCK_ROOT,false);
		if(rootStat == null){
			String rootNode = zk.create(LOCK_ROOT,new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT);
			logger.info("注册根节点成功！rootNode: "+rootNode);
		}
		//在zookeeper上注册子节点（锁）
		lockName = zk.create(LOCK_ROOT+"/lock_",new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.EPHEMERAL_SEQUENTIAL);
		logger.info("注册子节点成功！childNode: "+lockName);
		// 获取LOCK_ROOT下所有的子节点，并按照节点序号排序
		List<String> lockPaths = zk.getChildren(LOCK_ROOT,false);
		Collections.sort(lockPaths);
		int index = lockPaths.indexOf(lockName.substring(LOCK_ROOT.length()+1));
		if(index == 0){
			return true;
		}else{
			// 创建Watcher，监控lockPath的前一个节点
			Watcher watcher = new Watcher() {
				public void process(WatchedEvent watchedEvent) {
                    synchronized (this){
                    	notifyAll();
					}
				}
			};
			// 查询前一个节点是否存在，并且注册节点事件监听器，监听它的删除事件
			String previousNode = lockPaths.get(index-1);
			Stat stat = zk.exists(LOCK_ROOT+"/"+previousNode,watcher);
			if(stat == null){  //不存在的话继续获取最小的
				return this.tryLock();
			}else{
				synchronized (watcher) {
					// 等待lockName的前一个节点删除事件唤醒
					watcher.wait();
				}
				return tryLock();
			}
		}
	}

}
