package com.lythen.kingkood.redis;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * redis获取并设置缓存的帮助类
 *
 * @Author lythen
 * @date 2024/11/8 11:24
 **/
@Component
public class CacheRedisHelper {
    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    RedissonClient redissonClient;


    /**
     * 获取缓存，如果不存在，则调用supplier方法获取，并设置缓存
     * @param key
     * @param lockKey
     * @param supplier
     * @return
     * @param <T>
     */
    public <T> T getAndSetCacheObjectIfNotExists(String key, String lockKey, Supplier<T> supplier) {
        T result = (T) redisTemplate.opsForValue().get(key);
        if (result == null) {
            Lock lock =  redissonClient.getLock(lockKey);
            try {
                lock.lock();
                result = (T) redisTemplate.opsForValue().get(key);
                if (result == null) {
                    result = supplier.get();
                    redisTemplate.opsForValue().set(key, result);
                }
            } finally {
                lock.unlock();
            }
        }
        return result;
    }

    /**
     * 获取缓存列表，如果不存在，则调用supplier方法获取，并设置缓存
     * @param key
     * @param lockKey
     * @param supplier
     * @return
     * @param <T>
     */
    public <T> List<T> getAndSetCacheListIfNotExists(String key, String lockKey, Supplier<List<T>> supplier) {
        List<T> result = redisTemplate.opsForList().range(key, 0L, -1L);

        if (result == null || result.isEmpty()) {

            result = tryRedisLockAction(lockKey,key,(data)->{
                List<T> temp = redisTemplate.opsForList().range(key, 0L, -1L);
                if (temp == null || temp.isEmpty()) {
                    temp = supplier.get();
                    if (temp !=null && !temp.isEmpty()) {
                        this.redisTemplate.opsForList().rightPushAll(key, temp);
                    }
                }
                return temp;
            });
        }
        return result;
    }

    /**
     * 通用的带锁的操作
     * @param lockKey 锁名称
     * @param data 数据
     * @param supplier 操作方法
     * @return
     * @param <T> 数据类型
     * @param <R> 返回值类型
     */
    public <T,R> R tryRedisLockAction(String lockKey,T data, Function<T,R> supplier){
        Lock lock =  redissonClient.getLock(lockKey);
        try{
            lock.lock();
            return supplier.apply(data);
        }finally {
            lock.unlock();
        }
    }
}
