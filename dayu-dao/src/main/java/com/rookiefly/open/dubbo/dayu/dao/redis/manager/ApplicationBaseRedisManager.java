package com.rookiefly.open.dubbo.dayu.dao.redis.manager;

import com.rookiefly.open.dubbo.dayu.common.redis.RedisClientTemplate;
import com.rookiefly.open.dubbo.dayu.common.redis.RedisKeyConstants;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 存应用对应的负责人
 */
@Component
public class ApplicationBaseRedisManager {

    @Resource
    private RedisClientTemplate redisClientTemplate;

    public void saveApplicationPhone(String appName, String phone) {
        String key = RedisKeyConstants.APP_PHONE_MAP_KEY;
        String field = appName;
        redisClientTemplate.setMapKey(key, field, phone);
    }

    public String getPhoneByAppName(String appName) {
        String key = RedisKeyConstants.APP_PHONE_MAP_KEY;
        return redisClientTemplate.getMapKey(key, appName);
    }

    public Map<String, String> getAllAppPhone() {
        return redisClientTemplate.getAllHash(RedisKeyConstants.APP_PHONE_MAP_KEY);
    }
}
