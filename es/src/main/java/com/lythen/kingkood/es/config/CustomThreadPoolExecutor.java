package com.lythen.kingkood.es.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 关于类的说明
 *
 * @Author lythen
 * @date 2023/11/9 11:12
 **/
@Configuration
public class CustomThreadPoolExecutor {
    @Autowired
    CustomElasticsearchConfig customElasticsearchConfig;
    /**
     * ES的线程池，用于执行ES的一些并发任务
     * @return
     */
    @Bean("esThreadPoolExecutor")
    public ThreadPoolExecutor threadPool() {
        if (customElasticsearchConfig.getCorePoolSize() == 0) {
            return null;
        }
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(customElasticsearchConfig.getCorePoolSize(), customElasticsearchConfig.getMaxPoolSize(), customElasticsearchConfig.getThreadKeepAliveTime(),
                TimeUnit.SECONDS, new LinkedBlockingDeque<>(customElasticsearchConfig.getThreadQueueSize()));
        return threadPoolExecutor;
    }
}
