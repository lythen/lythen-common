package com.lythen.kingkood.core.http;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * 关于类的说明
 *
 * @Author lythen
 * @date 2024/11/1 11:55
 **/
@Component
@ConfigurationProperties(prefix = "http.client")
@Data
public class HttpClientConfig {
    /**
     * 超时时间毫秒，默认30秒
     */
    int timeout = 30000;

    HttpClient httpClient;

    @PostConstruct
    public void Init(){
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeout)).build();
    }
}
