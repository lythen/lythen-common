package com.lythen.kingkood.es.config;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 关于类的说明
 *
 * @Author lythen
 * @date 2023/11/3 15:47
 **/
@Configuration
public class LocalCacheConfig {
    @Bean("elasticSearchQueryCache")
    public Cache<String,Object> ElasticSearchQueryCache(){
        Cache<String, Object> cache = CacheBuilder.newBuilder()
                .maximumSize(100000) // 设置缓存的最大容量
                .expireAfterAccess(Duration.ofDays(1))// 设置缓存的有效期
                .build();
        return cache;
    }
}
