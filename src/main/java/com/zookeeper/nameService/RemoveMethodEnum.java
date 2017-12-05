package com.zookeeper.nameService;

/**
 * 删除策略
 *
 * Created by wangruli on 2017/10/10.
 */

public enum RemoveMethodEnum {
    NONE,
    /** 立刻删除 */
    IMMEDIATELY,
    /** 延迟删除 */
    DELAY
}
