package com.zookeeper.master;

import java.io.Serializable;

/**
 * @author wangruli
 * @description
 * @date 2017/10/12 17:38
 */
public class RunningData implements Serializable {

	 private static final long serialVersionUID = 4260577459043203630L;

	 //服务器id
	 private long cid;
	 //服务器名称
	 private String name;

	 private String port;

	public long getCid() {
		return cid;
	}

	public void setCid(long cid) {
		this.cid = cid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}
}
