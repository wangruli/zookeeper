package com.zookeeper.publicAndSubscribe;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;

import java.util.List;

/**
 * @author wangruli
 * @description
 * @date 2017/10/12 15:37
 */
public class Consumer {
	private String serversPath;
	private ZkClient zkClient;
	//用于监听zookeeper中servers节点的子节点列表变化
	private IZkChildListener childListener;
	//provider的列表
	private List<String> providerServerList;

	public Consumer() {
	}

	public Consumer(String serversPath, ZkClient zkClient) {
		super();
		this.serversPath = serversPath;
		this.zkClient = zkClient;
		this.childListener = new IZkChildListener() {
			public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
				providerServerList = currentChilds;
				System.out.println("provider server list changed, new list is ");
				execList();
			}
		};
	}

	private void execList() {
		System.out.println(providerServerList.toString());
	}

	public void start() {
		initRunning();
	}

	public void stop() {
		//取消订阅servers节点的列表变化
		zkClient.unsubscribeChildChanges(serversPath, childListener);
	}

	/**
	 * 初始化
	 */
	private void initRunning() {
		//执行订阅servers节点的列表变化
		zkClient.subscribeChildChanges(serversPath, childListener);
	}

}
