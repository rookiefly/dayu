package com.rookiefly.open.dubbo.dayu.common.redis;

import redis.clients.jedis.ShardedJedis;

/**
 * redis最底层，从pool中获得shardedJsdis对象
 */
public interface RedisDataSource {

    ShardedJedis getRedisClient();

    void returnResource(ShardedJedis shardedJedis, boolean broken);
}
