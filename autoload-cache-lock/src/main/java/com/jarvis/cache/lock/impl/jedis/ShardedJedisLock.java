package com.jarvis.cache.lock.impl.jedis;

import com.jarvis.cache.lock.api.redis.AbstractRedisLock;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.params.SetParams;

/**
 * ShardedJedisPool是基于一致性哈希算法实现的分布式jedis池
 *
 * @author nobody
 */
public class ShardedJedisLock extends AbstractRedisLock {

    private ShardedJedisPool shardedJedisPool;

    public ShardedJedisLock(ShardedJedisPool shardedJedisPool) {
        this.shardedJedisPool = shardedJedisPool;
    }

    private void returnResource(ShardedJedis shardedJedis) {
        shardedJedis.close();
    }

    @Override
    protected boolean setnx(String key, String val, int expire) {
        ShardedJedis shardedJedis = null;
        try {
            shardedJedis = shardedJedisPool.getResource();
            Jedis jedis = shardedJedis.getShard(key);
            return OK.equalsIgnoreCase(jedis.set(key, val, SetParams.setParams().nx().ex(expire)));
        } finally {
            returnResource(shardedJedis);
        }
    }

    @Override
    protected void del(String key) {
        ShardedJedis shardedJedis = null;
        try {
            shardedJedis = shardedJedisPool.getResource();
            Jedis jedis = shardedJedis.getShard(key);
            jedis.del(key);
        } finally {
            returnResource(shardedJedis);
        }
    }
}
