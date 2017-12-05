package com.zookeeper.publicAndSubscribe;

import com.alibaba.fastjson.JSON;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNoNodeException;

/**
 * @author wangruli
 * @description 服务提供者，注册服务到serversPath下
 * @date 2017/10/12 15:29
 */
public class Provider {
	private String serversPath;
	private ZkClient zkClient;
	private ServerData serverData;

	public Provider() {}

	/**
	 *
	 * @param serversPath provider要存的地址
	 * @param zkClient ZooKeeper连接
	 * @param serverData provider要存的信息
	 */
	public Provider(String serversPath, ZkClient zkClient, ServerData serverData) {
		super();
		this.serversPath = serversPath;
		this.zkClient = zkClient;
		this.serverData = serverData;
	}

	public void start(){
		if(!zkClient.exists(serversPath)){
			zkClient.createPersistent(serversPath);
		}

		registMeToZookeeper();
	}

	/**
	 * 启动时向zookeeper注册自己
	 */
	private void registMeToZookeeper(){
		//向zookeeper中注册自己的过程其实就是向servers节点下注册一个临时节点
		//构造临时节点
		String mePath = serversPath.concat("/").concat(serverData.getAddress());
		try{
			//存入是将json序列化
			zkClient.createEphemeral(mePath, JSON.toJSONString(serverData).getBytes());
		} catch (ZkNoNodeException e) {
			//父节点不存在
			zkClient.createPersistent(serversPath, true);
			registMeToZookeeper();
		}

	}
}
