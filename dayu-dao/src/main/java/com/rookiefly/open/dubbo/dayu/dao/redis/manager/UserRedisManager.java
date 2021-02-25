package com.rookiefly.open.dubbo.dayu.dao.redis.manager;

import com.rookiefly.open.dubbo.dayu.common.redis.RedisClientTemplate;
import com.rookiefly.open.dubbo.dayu.common.redis.RedisKeyBean;
import com.rookiefly.open.dubbo.dayu.common.tools.TimeUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

@Service
public class UserRedisManager {

    @Resource
    private RedisClientTemplate redisClientTemplate;

    public void saveUserIpName(String ip, String name) {
        String mapKey = String.format(RedisKeyBean.userIpNameKey, TimeUtil.getDateString(new Date()));
        String ipNameField = String.format(RedisKeyBean.userIpNameFieldKey, ip);

        redisClientTemplate.setMapKey(mapKey, ipNameField, name);
    }

    public String getUserIPName(String ip) {
        String mapKey = String.format(RedisKeyBean.userIpNameKey, TimeUtil.getDateString(new Date()));
        String ipNameField = String.format(RedisKeyBean.userIpNameFieldKey, ip);

        return redisClientTemplate.getMapKey(mapKey, ipNameField);
    }
}
