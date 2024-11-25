package com.lythen.kingkood.redis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 关于类的说明
 *
 * @Author lythen
 * @date 2024/10/21 16:26
 **/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisDifferLock {
    /**
     * 锁定的值
     * @return
     */
    String value();

    /**
     * 锁键存放目录名，无特殊需求可不设置
     * @return
     */
    String locker() default "";

    String errorMsg() default "";

    /**
     * 是否当有锁时， 是否抛出异常
     * @return
     */
    boolean throwException() default true;

    /**
     * 最长锁定时间
     * @return
     */
    long expire() default 30;
}
