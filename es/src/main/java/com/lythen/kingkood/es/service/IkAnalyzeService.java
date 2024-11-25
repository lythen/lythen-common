package com.lythen.kingkood.es.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.AnalyzeRequest;
import org.elasticsearch.client.indices.AnalyzeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * 关于类的说明
 *
 * @Author lythen
 * @date 2023/11/3 15:03
 **/
@Component
public class IkAnalyzeService {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;
    final private String BASEKEY = "es:analyze:cache:";

    public IkAnalyzeService() {

    }

    /**
     * 调用 ES 获取 IK 分词后结果
     *
     * @param strMd5        请求ID，根据请求id将分词写入redis
     * @param ikAnalyzer    分词方法，ik_smart或者ik_max_word
     * @param searchContent 待分词文本
     */
    public Set<String> createIkAnalyzeSearchTerms(String strMd5, String ikAnalyzer, String searchContent) {
        AnalyzeResponse analyzeResponse = elasticsearchRestTemplate.execute(
                client -> {
                    AnalyzeRequest analyzeRequest = AnalyzeRequest.withGlobalAnalyzer(ikAnalyzer, searchContent);
                    return client.indices().analyze(analyzeRequest, RequestOptions.DEFAULT);
                }
        );
        Set<String> resultTokens = new HashSet<>();
        analyzeResponse.getTokens().forEach(analyzeToken -> {
            resultTokens.add(analyzeToken.getTerm());
        });
        if (redisTemplate != null && !resultTokens.isEmpty()) {
            String key = BASEKEY + strMd5;
            redisTemplate.opsForSet().add(key, resultTokens.toArray(new String[resultTokens.size()]));
            redisTemplate.expire(key, Duration.ofDays(1));
        }
        return resultTokens;
    }

    /**
     * 调用 ES 获取 IK 分词后结果
     * 会先计算文本的MD5值，如果hash值存在内存中，则直接从内存取
     * 如果有redis，则使用缓存
     *
     * @param ikAnalyzer    分词方法，ik_smart或者ik_max_word
     * @param searchContent 待分词文本
     */
    public Set<String> getIkAnalyzeSearchTerms(String ikAnalyzer, String searchContent) {
        String strMd5 = MD5.create().digestHex(searchContent + ikAnalyzer, StandardCharsets.UTF_8);
        if (redisTemplate == null) {
            return createIkAnalyzeSearchTerms(strMd5, ikAnalyzer, searchContent);
        }
        String key = BASEKEY + strMd5;
        Set<String> resultTokens = redisTemplate.opsForSet().members(key);
        if (resultTokens != null && !resultTokens.isEmpty()) {
            return resultTokens;
        }
        return createIkAnalyzeSearchTerms(strMd5, ikAnalyzer, searchContent);
    }

    /**
     * 调用 ES 获取 IK 分词后结果
     *
     * @param indexName     索引名称
     * @param strMd5        请求ID，根据请求id将分词写入redis
     * @param ikAnalyzer    分词方法，ik_smart或者ik_max_word
     * @param searchContent 待分词文本
     */
    public Set<String> createIkAnalyzeSearchTerms(String indexName, String strMd5, String ikAnalyzer, String searchContent) {
        if (StrUtil.isBlank(indexName)) {
            return createIkAnalyzeSearchTerms(strMd5, ikAnalyzer, searchContent);
        }
        AnalyzeResponse analyzeResponse = elasticsearchRestTemplate.execute(
                client -> {
                    AnalyzeRequest analyzeRequest = AnalyzeRequest.withGlobalAnalyzer(indexName, ikAnalyzer, searchContent);
                    return client.indices().analyze(analyzeRequest, RequestOptions.DEFAULT);
                }
        );
        Set<String> resultTokens = new HashSet<>();
        analyzeResponse.getTokens().forEach(analyzeToken -> {
            resultTokens.add(analyzeToken.getTerm());
        });
        if (redisTemplate != null && !resultTokens.isEmpty()) {
            String key = BASEKEY + strMd5;
            redisTemplate.opsForSet().add(key, resultTokens.toArray(new String[resultTokens.size()]));
            redisTemplate.expire(key, Duration.ofDays(1));
        }
        return resultTokens;
    }

    /**
     * 调用 ES 获取 IK 分词后结果
     * 会先计算文本的MD5值，如果hash值存在内存中，则直接从内存取
     * 如果有redis，则使用缓存
     *
     * @param indexName     索引名称
     * @param ikAnalyzer    分词方法，ik_smart或者ik_max_word
     * @param searchContent 待分词文本
     */
    public Set<String> getIkAnalyzeSearchTerms(String indexName, String ikAnalyzer, String searchContent) {
        if (StrUtil.isBlank(indexName)) {
            return getIkAnalyzeSearchTerms(ikAnalyzer, searchContent);
        }
        String strMd5 = MD5.create().digestHex(indexName + searchContent + ikAnalyzer, StandardCharsets.UTF_8);
        if (redisTemplate == null) {
            return createIkAnalyzeSearchTerms(indexName, strMd5, ikAnalyzer, searchContent);
        }
        String key = BASEKEY + strMd5;
        Set<String> resultTokens = redisTemplate.opsForSet().members(key);
        if (resultTokens != null && !resultTokens.isEmpty()) {
            return resultTokens;
        }
        return createIkAnalyzeSearchTerms(indexName, strMd5, ikAnalyzer, searchContent);
    }
}
