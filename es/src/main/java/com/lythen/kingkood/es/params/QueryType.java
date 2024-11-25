package com.lythen.kingkood.es.params;

/**
 * 关于类的说明
 *
 * @Author lythen
 * @date 2023/11/9 9:34
 **/
public enum QueryType
{
    /**
     * 精确查询
     */
    TERM,
    /**
     * 前缀查询
     */
    MATCH_PHRASE_PREFIX,
    /**
     * 后缀查询
     */
    SUFFIX,
    /**
     * 范围查询
     */
    RANGE,
    /**
     * 匹配查询
     */
    MATCH,
    /**
     * 匹配查询
     */
    MATCH_PHRASE,
    /**
     * 匹配查询
     */
    MATCH_ALL,
}
