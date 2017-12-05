package com.zookeeper.config;

import java.util.List;

/**
 * 配置改变的订阅者，在每一個zk文件上订阅一個监听器
 * 
 * @author june
 *
 */
public interface ConfigChangeSubscriber {
	 String getInitValue(String paramString);

	 void subscribe(String paramString, ConfigChangeListener paramConfigChangeListener);

	 List<String> listKeys();
}