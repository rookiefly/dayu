package com.rookiefly.open.dubbo.dayu.dao.redis.manager;

import com.rookiefly.open.dubbo.dayu.common.redis.RedisClientTemplate;
import com.rookiefly.open.dubbo.dayu.common.redis.RedisKeyBean;
import com.rookiefly.open.dubbo.dayu.common.tools.JsonUtil;
import com.rookiefly.open.dubbo.dayu.model.bo.ApplicationChangeBO;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Component
public class AppStopRedisManagerImpl implements AppStopRedisManager {

    @Resource
    private RedisClientTemplate redisClientTemplate;

    @Override
    public Map<ApplicationChangeBO, String> getAllStopApp() {
        Map<ApplicationChangeBO, String> resultMap = new HashMap<>();
        for (Map.Entry<String, String> entry : redisClientTemplate.getAllHash(RedisKeyBean.appStopMapKey).entrySet()) {
            String key = entry.getKey();
            ApplicationChangeBO applicationChangeBO = JsonUtil.jsonStrToObject(key, ApplicationChangeBO.class);
            resultMap.put(applicationChangeBO, entry.getValue());
        }
        return resultMap;
    }

    @Override
    public void saveStopApp(ApplicationChangeBO applicationChangeBO, Integer number) {
        if (null == number) {
            number = 0;
        }
        ApplicationChangeBO filedBO = new ApplicationChangeBO();
        filedBO.setAppName(applicationChangeBO.getAppName());
        filedBO.setHost(applicationChangeBO.getHost());
        filedBO.setPort(applicationChangeBO.getPort());


        String filed = JsonUtil.objectToJsonStr(filedBO);
        String value = applicationChangeBO.getTime() + "," + number;
        redisClientTemplate.setMapKey(RedisKeyBean.appStopMapKey, filed, value);
    }

    @Override
    public void removeStopApp(ApplicationChangeBO applicationChangeBO) {
        ApplicationChangeBO filedBO = new ApplicationChangeBO();
        filedBO.setAppName(applicationChangeBO.getAppName());
        filedBO.setHost(applicationChangeBO.getHost());
        filedBO.setPort(applicationChangeBO.getPort());

        String filed = JsonUtil.objectToJsonStr(filedBO);
        redisClientTemplate.delMapKey(RedisKeyBean.appStopMapKey, filed);
    }
}
