package com.lythen.kingkood.core.http;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 关于类的说明
 *
 * @Author lythen
 * @date 2024/11/1 11:43
 **/
@Slf4j
@Component
public class HttpRequestGet extends HttpRequestBase{
    @Autowired
    HttpClientConfig httpClientConfig;

    public JSONObject get(String url) {
        return get(url, httpClientConfig.timeout);
    }

    public JSONObject get(String url, int timeout) {
        URI uri = URI.create(url);

        var request = HttpRequest.newBuilder().uri(uri)
                //form表单则使用下面的配置
                .header("Content-Type", "text/plain")
                .header("Content-Encoding", "utf-8")
                .timeout(Duration.ofMillis(timeout))
                .GET()
                .build();
        try {
            var response = httpClientConfig.httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String result = response.body();
            return JSONObject.parseObject(result);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("请求失败。", e);
        }
        return null;
    }
}
