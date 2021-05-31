package com.rookiefly.open.dubbo.dayu.dao.redis.manager;

import com.rookiefly.open.dubbo.dayu.common.redis.RedisClientTemplate;
import com.rookiefly.open.dubbo.dayu.common.redis.RedisKeyConstants;
import com.rookiefly.open.dubbo.dayu.common.tools.TimeUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

@Service
public class UserRedisManager {

    @Resource
    private RedisClientTemplate redisClientTemplate;

    public void saveUserIpName(String ip, String name) {
        String mapKey = String.format(RedisKeyConstants.USER_IP_NAME_KEY, TimeUtil.getDateString(new Date()));
        String ipNameField = String.format(RedisKeyConstants.USER_IP_NAME_FIELD_KEY, ip);

        redisClientTemplate.setMapKey(mapKey, ipNameField, name);
    }

    public String getUserIPName(String ip) {
        String mapKey = String.format(RedisKeyConstants.USER_IP_NAME_KEY, TimeUtil.getDateString(new Date()));
        String ipNameField = String.format(RedisKeyConstants.USER_IP_NAME_FIELD_KEY, ip);

        return redisClientTemplate.getMapKey(mapKey, ipNameField);
    }
}
