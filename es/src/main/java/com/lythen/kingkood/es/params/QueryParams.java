package com.lythen.kingkood.es.params;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lythen.kingkood.core.time.LocalDateTimeDeserializer;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 关于类的说明
 * 查询实体
 *
 * @Author lythen
 * @date 2023/6/5 12:09
 **/
@Data
@Builder
public class QueryParams implements Serializable, Cloneable {
    /**
     * 索引名称
     */
    String indexName;
    /**
     * 关键字
     */
    String keyword;
    /**
     * 关键字字段名
     */
    String keywordField = "keyword";
    /**
     * 起始时间
     */
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    LocalDateTime startDate;
    /**
     * 结束时间
     */
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    LocalDateTime endDate;
    /**
     * id 使用ID时为精确查询
     */
    String id;
    /**
     * 查询ID，主要用于分页查询，使用分页查询时该ID用于获取已有的分词结果
     */
    String queryId;
    /**
     * ywid，使用业务ID时为精确查询
     */
    String ywid;
    /**
     * 排序规则，k:字段 V：规则 DESC或者ASC
     */
    Map<String, String> sort;
    /**
     * 分页大小
     */
    int pageSize;
    /**
     * 页码，从0开始
     */
    int pageIndex;
    /**
     * 高亮字段
     */
    List<String> highlightFields;
    /**
     * 查询返回指定的字段
     */
    String[] includes;
    /**
     * 查询结果中排除返回的字段
     */
    String[] excludes;
    /**
     * 自定义的字段查询
     */
    List<CustomQueryField> queryFieldsList;

    public void addQueryField(CustomQueryField customQueryField) {
        if (this.queryFieldsList == null) {
            this.queryFieldsList = new ArrayList<>();
        }
        this.queryFieldsList.add(customQueryField);
    }

    public void addHighlightField(String field) {
        if (this.highlightFields == null) {
            this.highlightFields = new ArrayList<>();
        }
        this.highlightFields.add(field);
    }

    @Override
    public QueryParams clone() {
        try {
            QueryParams clone = (QueryParams) super.clone();
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
