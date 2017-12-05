package com.zookeeper.publicAndSubscribe;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.BytesPushThroughSerializer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wangruli
 * @description
 * @date 2017/10/12 15:38
 */
public class ConfigServerMain {
	private static final String  ZOOKEEPER_SERVER = "192.168.0.33:2181";
	private static final String SERVERS_PATH = "/servers";

	public static void main(String[] args) {
		//用来存储所有的clients,最后close使用
		List<ZkClient> clients = new ArrayList<ZkClient>();
		Consumer consumer = null;
		try {
			ZkClient clientManage = new ZkClient(ZOOKEEPER_SERVER, 5000, 5000, new BytesPushThroughSerializer());
			consumer = new Consumer(SERVERS_PATH,clientManage);
			consumer.start();

			for(int i = 0; i < 5; i++){
				ZkClient client = new ZkClient(ZOOKEEPER_SERVER, 5000, 5000, new BytesPushThroughSerializer());
				clients.add(client);
				ServerData serverData = new ServerData();
				serverData.setAddress("192.168.0.3" +i);
				serverData.setId(i);
				serverData.setName("provider&&" + i);
				Provider provider = new Provider(SERVERS_PATH, client, serverData);
				provider.start();

				System.out.println("敲回车键继续添加provider！\n");
				new BufferedReader(new InputStreamReader(System.in)).readLine();
			}
			System.out.println("敲回车键退出！\n");
			new BufferedReader(new InputStreamReader(System.in)).readLine();
		} catch (Exception e) {
            e.printStackTrace();
		}finally {
			for (ZkClient zkClient : clients) {
				zkClient.close();
			}
		}

	}
}
