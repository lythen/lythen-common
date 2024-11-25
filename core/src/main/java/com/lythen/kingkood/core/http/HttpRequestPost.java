package com.lythen.kingkood.core.http;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 基于jdk11的http post请求
 *
 * @Author lythen
 * @date 2024/11/1 11:13
 **/
@Slf4j
@Component
public class HttpRequestPost extends HttpRequestBase {
    @Autowired
    HttpClientConfig httpClientConfig;

    /**
     * 发送时间，使用默认的超时间
     *
     * @param url   请求地址
     * @param files 文件名列表
     * @return
     * @throws IOException
     */
    public JSONObject PostFile(String url, String[] files) throws IOException, InterruptedException {
        return PostFile(url, httpClientConfig.timeout, files);
    }

    /**
     * 文件发送
     *
     * @param url     请求地址
     * @param timeout 请求超时时间
     * @param files   文件名列表
     * @return
     * @throws IOException
     */
    public JSONObject PostFile(String url, int timeout, String[] files) throws IOException, InterruptedException {
        URI uri = URI.create(url);
        //设置建立连接的超时 connect timeout
        var builder = MultipartEntityBuilder.create()
                .setMode(HttpMultipartMode.RFC6532);
        for (String filepath : files) {
            File file = new File(filepath);
            if (!file.exists()) {
                throw new FileNotFoundException(String.format("文件%s不存在", filepath));
            }
            builder.addBinaryBody("file", file, ContentType.getByMimeType("multipart/form-data"), file.getName())
                    .build();
        }

        HttpEntity httpEntity = builder.build();
        Pipe pipe = Pipe.open();
        new Thread(() -> {
            try (OutputStream outputStream = Channels.newOutputStream(pipe.sink())) {
                // Write the encoded data to the pipeline.
                httpEntity.writeTo(outputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }).start();

        var request = HttpRequest.newBuilder().uri(uri)
                //form表单则使用下面的配置
                .header("Content-Type", httpEntity.getContentType().getValue())
                .header("Content-Encoding", "utf-8")
                .timeout(Duration.ofMillis(timeout))
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> Channels.newInputStream(pipe.source()))).build();

        var response = httpClientConfig.httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String result = response.body();
        return JSONObject.parseObject(result);
    }
}
