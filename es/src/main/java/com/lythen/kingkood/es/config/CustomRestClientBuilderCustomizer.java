package com.lythen.kingkood.es.config;

import org.apache.http.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.autoconfigure.elasticsearch.RestClientBuilderCustomizer;
import org.springframework.data.elasticsearch.client.ClientLogger;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 输出ES查询的DDL语句
 *
 * @Author lythen
 * @date 2023/7/13 10:03
 **/
@Component
public class CustomRestClientBuilderCustomizer implements RestClientBuilderCustomizer {

    /**
     * Name of whose value can be used to correlate log messages for this request.
     */
    private static final String LOG_ID_ATTRIBUTE = RestClients.class.getName() + ".LOG_ID";


    @Override
    public void customize(RestClientBuilder builder) {

    }

    @Override
    public void customize(HttpAsyncClientBuilder builder) {
        if (ClientLogger.isEnabled()) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();

            builder.addInterceptorLast((HttpRequestInterceptor) interceptor);
            builder.addInterceptorLast((HttpResponseInterceptor) interceptor);
        }
    }

    /**
     * 由于自动装配 RestHighLevelClient 没有使用 spring data elasticsearch 相关的类，导致打印日志不能兼容
     *
     */
    private static class HttpLoggingInterceptor implements HttpResponseInterceptor, HttpRequestInterceptor {

        @Override
        public void process(HttpRequest request, HttpContext context) throws IOException {

            String logId = (String) context.getAttribute(LOG_ID_ATTRIBUTE);

            if (logId == null) {
                logId = ClientLogger.newLogId();
                context.setAttribute(LOG_ID_ATTRIBUTE, logId);
            }

            if (request instanceof HttpEntityEnclosingRequest && ((HttpEntityEnclosingRequest) request).getEntity() != null) {

                HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) request;
                HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                entity.writeTo(buffer);

                if (!entity.isRepeatable()) {
                    entityRequest.setEntity(new ByteArrayEntity(buffer.toByteArray()));
                }

                ClientLogger.logRequest(logId, request.getRequestLine().getMethod(), request.getRequestLine().getUri(), "",
                        buffer::toString);
            } else {
                ClientLogger.logRequest(logId, request.getRequestLine().getMethod(), request.getRequestLine().getUri(), "");
            }
        }

        @Override
        public void process(HttpResponse response, HttpContext context) {
            String logId = (String) context.getAttribute(LOG_ID_ATTRIBUTE);
            ClientLogger.logRawResponse(logId, HttpStatus.resolve(response.getStatusLine().getStatusCode()));
        }
    }

}
