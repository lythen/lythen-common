package com.lythen.kingkood.es.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


/**
 * 关于类的说明
 *
 * @Author lythen
 * @date 2023/11/9 10:56
 **/
@ConfigurationProperties(prefix = "spring.elasticsearch.custom")
@Component
@Data
public class CustomElasticsearchConfig {
    int maxPoolSize;
    int corePoolSize;
    int threadQueueSize;
    int threadKeepAliveTime;
    /**
     * 是否自动搜索，主要用于实现跳页功能
     */
    boolean autoSearch;
    /**
     * 自动搜索的页数
     */
    int autoSearchPageCount;
    /**
     * 高亮颜色
     */
    String highlightColor;
    /**
     * 自动搜索的类
     */
    Class[] autoSearchClass;

    public String getHighlightColor() {
        if (highlightColor == null) {
            highlightColor = "red";
        }
        return highlightColor;
    }

    public int getAutoSearchPageCount() {
        if (autoSearchPageCount == 0) {
            autoSearchPageCount = 10;
        }
        return autoSearchPageCount;
    }

    public int getCorePoolSize() {
        if (corePoolSize == 0) {
            corePoolSize = Runtime.getRuntime().availableProcessors();
        }
        return corePoolSize;
    }

    public int getMaxPoolSize() {
        if (maxPoolSize == 0) {
            maxPoolSize = getCorePoolSize() * 2;
        }
        return maxPoolSize;
    }
    public int getThreadQueueSize() {
        if (threadQueueSize == 0) {
            threadQueueSize = 1000;
        }
        return threadQueueSize;
    }
    public int getThreadKeepAliveTime() {
        if (threadKeepAliveTime == 0) {
            threadKeepAliveTime = 60;
        }
        return threadKeepAliveTime;
    }
}
