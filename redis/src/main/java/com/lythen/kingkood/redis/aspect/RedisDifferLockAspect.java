package com.lythen.kingkood.redis.aspect;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.lythen.kingkood.redis.annotation.RedisDifferLock;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * redis排他锁
 *
 * @Author lythen
 * @date 2024/10/21 16:25
 **/
@Aspect
@Component
public class RedisDifferLockAspect {

    @Autowired
    RedissonClient redissonClient;

    String catalog = "lythen:lock:";

    @Around("@annotation(redisLock)")
    public Object checkRedisLock(ProceedingJoinPoint joinPoint, RedisDifferLock redisLock) throws Throwable {
        String lockKey = resolveLockKey(joinPoint, redisLock);
        // 如果为空，则不锁。
        if (StrUtil.isBlank(lockKey)) {
            return joinPoint.proceed();
        }
        Lock lock = redissonClient.getLock(lockKey);
        boolean success = lock.tryLock(0, TimeUnit.MILLISECONDS);
        if (success) {
            try {
                return joinPoint.proceed();
            } finally {
                lock.unlock();
            }
        } else {
            if (redisLock.throwException()) {
                throw new RuntimeException(StrUtil.isBlank(redisLock.errorMsg()) ? ("目标已被锁定，请等待操作完成。" + lockKey) : redisLock.errorMsg());
            } else {
                return null;
            }
        }
    }

    private String resolveLockKey(ProceedingJoinPoint joinPoint, RedisDifferLock redisLock) throws NoSuchFieldException {
        String value = redisLock.value();
        Object[] args = joinPoint.getArgs();

        if (args.length > 0 && value.contains(".") && value.matches(".*\\.\\w+")) {
            // 动态解析属性，例如 value = "user.username"
            String[] parts = value.split("\\.");
            // 这里先置空，后面如果为空，也就是没找到，就返回空值，不能返回原来的value，因为那样可能会锁很大的范围。
            value = "";
            if (parts.length == 2) {
                String argName = parts[0];
                String fieldName = parts[1];
                try {
                    Object arg = Arrays.stream(args).filter(a -> a.getClass().getSimpleName().equalsIgnoreCase(argName)).findFirst().orElse(null);
                    if (arg != null) {
                        Object val = ReflectUtil.getFieldValue(arg, fieldName);
                        if (val != null) {
                            value = val.toString();
                        }
                    }
                } catch (Exception e) {

                }
            }
        }

        // 如果解析失败或value是静态字符串，直接返回value
        return StrUtil.isBlank(value) ? "" : catalog + (StrUtil.isBlank(redisLock.locker()) ? "" : redisLock.locker() + ":") + value;
    }
}
