package com.rookiefly.open.dubbo.dayu.common.redis;

import com.rookiefly.open.dubbo.dayu.common.tools.SpringContextsUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.stereotype.Component;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

import javax.annotation.Resource;

@Component
@Slf4j
public class RedisDataSourceImpl implements RedisDataSource {

    @Resource
    private ShardedJedisPool shardedJedisPool;

    @Override
    public ShardedJedis getRedisClient() {
        try {
            if (null == shardedJedisPool) {
                shardedJedisPool = (ShardedJedisPool) SpringContextsUtil.getBean("shardedJedisPool");
            }
            ShardedJedis shardJedis = shardedJedisPool.getResource();
            return shardJedis;
        } catch (Exception e) {
            log.error("getRedisClent error", e);
        }
        return null;
    }

    @Override
    public void returnResource(ShardedJedis shardedJedis, boolean broken) {
        try {
            shardedJedis.close();
        } catch (BeansException e) {
            log.error("returnResource error", e);
        }
    }
}
