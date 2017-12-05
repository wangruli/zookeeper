package com.zookeeper.config;

/**
 * 监听器，监听配置的改变
 * 
 * @author june
 * 
 */
public abstract interface ConfigChangeListener {
	void configChanged(String paramString1, String paramString2);
}