package com.rookiefly.open.dubbo.dayu.dao.redis.manager;

import com.rookiefly.open.dubbo.dayu.model.bo.ApplicationChangeBO;

import java.util.Map;

/**
 * 内存中的记录 服务停止
 */
public interface AppStopRedisManager {

    /**
     * 获得所有的数据
     *
     * @return
     */
    Map<ApplicationChangeBO, String> getAllStopApp();

    /**
     * 保存停止服务（删除）的当前记录于当前内存中
     *
     * @param applicationChangeBO
     * @param number
     */
    void saveStopApp(ApplicationChangeBO applicationChangeBO, Integer number);

    /**
     * 移除已经启动的服务于停止列表
     *
     * @param applicationChangeBO
     */
    void removeStopApp(ApplicationChangeBO applicationChangeBO);
}
